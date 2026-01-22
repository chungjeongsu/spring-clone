package springinfra.context.di.bdrpp;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;

public class ClassPathBeanDefinitionScanner {
    private final ClassLoader classLoader;
    private final BeanDefinitionRegistry beanDefinitionRegistry;

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry beanDefinitionRegistry) {
        this.beanDefinitionRegistry = beanDefinitionRegistry;
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public void scan(List<String> basePackages) {
        for(String basePackage : basePackages) {
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
        }
    }

    private Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> beanDefinitions = new LinkedHashSet<>();

        String basePackagePath = basePackage.replace('.', '/');

        try{
            Enumeration<URL> resources = classLoader.getResources(basePackagePath);

            while(resources.hasMoreElements()) {
                URL url = resources.nextElement();
                doScan(url, basePackage, beanDefinitions);
            }

            return beanDefinitions;
        } catch (IOException e) {
            throw new BeanDefinitionScanException("basePackage 리소스 로딩 중 예외가 발생했습니다.", e);
        }
    }

    private void doScan(URL url, String basePackage, Set<BeanDefinition> beanDefinitions) {
        try{
            if (url.getProtocol().equals("file")) {  //파일 시스템 파일 일 시
                doScanFile(url.toURI(), basePackage, beanDefinitions);
                return;
            }
            if(url.getProtocol().equals("jar")) {   //배포 jar일 시
                JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                doScanJar(jarURLConnection, basePackage, beanDefinitions);
            }
        } catch (URISyntaxException | IOException e) {
            throw new BeanDefinitionScanException("scan 준비 중 예외가 발생했습니다.", e);
        }
    }

    private void doScanFile(URI uri, String currPackage, Set<BeanDefinition> beanDefinitions) {
        File file = new File(uri);
        File[] files = file.listFiles();

        if(!file.isDirectory()) return;
        if(files == null) throw new BeanDefinitionScanException("파일 시스템 파일 스캔 중 예외가 발생했습니다.");

        for(File child : files) {
            if(child.isDirectory()) {
                String subPackage = currPackage + "." + child.getName();
                doScanFile(child.toURI(), subPackage, beanDefinitions);
                continue;
            }

            if(!child.getName().endsWith(".class") || child.getName().contains("$")) continue;

            String childSimpleName = child.getName().substring(0, child.getName().length() - 6);
            String className = currPackage + "." + childSimpleName;

            try {
                Class<?> clazz = classLoader.loadClass(className);

                BeanDefinition parsedBeanDefinition = beanDefinitionParser.parse(clazz);
                if (parsedBeanDefinition != null)
                    beanDefinitions.add(parsedBeanDefinition);
            } catch (ClassNotFoundException e) {
                throw new BeanDefinitionScanException("파일 시스템 파일 스캔 중 예외가 발생했습니다.", e);
            }
        }
    }

    //jar 파일 스캔
    private void doScanJar(JarURLConnection jarURLConnection, String basePackage, Set<BeanDefinition> beanDefinitions) {
        try(JarFile jarFile = jarURLConnection.getJarFile()){
            Enumeration<JarEntry> entries = jarFile.entries();
            while(entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (entry.isDirectory())
                    continue;
                if (!name.endsWith(".class") || name.contains("$"))
                    continue;
                if (!name.startsWith(basePackage))
                    continue;

                String className = name.substring(0, name.length() - 6).replace('/', '.');
                Class<?> clazz = classLoader.loadClass(className);
                BeanDefinition parsedBeanDefinition = beanDefinitionParser.parse(clazz);
                if (parsedBeanDefinition != null) beanDefinitions.add(parsedBeanDefinition);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new BeanDefinitionScanException("jar 파일 스캔 중 예외가 발생했습니다.", e);
        }
    }
}
