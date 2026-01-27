package springinfra.context.di.bdrpp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
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

import springinfra.annotation.Component;
import springinfra.annotation.Configuration;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.ConfigurationBeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition;
import springinfra.context.di.exception.BeanScanException;

/**
 * classLoader를 통해 ComponentScan 패키지를 기준으로 스캔
 * 아래는 패키지, 파일 등 경로 포멧
 * 리플렉션 getPackage() : springinfra.context.di.bdrpp
 * ClassLoader 넣는 값 : springinfra/context/di/bdrpp => .class 6 size char 제외
 * URL(디렉토리 url = getResources) : file:/Users/me/...
 * URL(jar url = getResources) : jar:file:/Users/me/...
 */
public class ClassPathBeanDefinitionScanner {
    private final ClassLoader classLoader;
    private final BeanDefinitionRegistry registry;
    private final BeanNameGenerator beanNameGenerator;
    private final BeanDefinitionTypeReader reader;

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry beanDefinitionRegistry) {
        this.registry = beanDefinitionRegistry;
        this.beanNameGenerator = new BeanNameGenerator();
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.reader = new BeanDefinitionTypeReader();
    }

    public void scan(List<String> basePackages) {
        for(String basePackage : basePackages) {

            Set<BeanDefinition> scannedBeanDefinitions = findCandidateComponents(basePackage);

            for(BeanDefinition bd : scannedBeanDefinitions) {
                registry.registerBeanDefinition(bd.getBeanName(), bd);
            }
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
            throw new BeanScanException("Resource를 가져오는데 예외가 발생했습니다. : " + basePackage);
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
            throw new BeanScanException("URL을 읽어오는데 문제가 생겼습니다. : " + basePackage);
        }
    }

    //DFS 방식으로 파일 구조 스캔
    private void doScanFile(URI uri, String currPackage, Set<BeanDefinition> beanDefinitions) {
        File file = new File(uri);
        File[] files = file.listFiles();

        if(!file.isDirectory()) return;
        if(files == null) throw new BeanScanException("파일 시스템 파일들이 NULL 입니다. : " + currPackage);

        for(File child : files) {
            if(child.isDirectory()) {
                String subPackage = currPackage + "." + child.getName();
                doScanFile(child.toURI(), subPackage, beanDefinitions);
                continue;
            }
            if(isRootNameFile(child.getName())) continue;

            try {
                String childFullName = getFullNameFormat(currPackage, child.getName());
                Class<?> beanClass = classLoader.loadClass(childFullName);

                BeanDefinition bd = reader.read(
                        beanClass,
                        beanNameGenerator.generateBeanName(beanClass, registry)
                );

                if (bd != null)
                    beanDefinitions.add(bd);
            } catch (ClassNotFoundException e) {
                throw new BeanScanException("스캔 이후 빈 생성 중 예외가 발생했습니다.");
            }
        }
    }

    //jar 파일 스캔
    private void doScanJar(JarURLConnection jarURLConnection, String basePackage, Set<BeanDefinition> beanDefinitions) {
        String basePackagePath = basePackage.replace('.', '/');

        try (JarFile jarFile = jarURLConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (entry.isDirectory()) continue;
                if (isRootNameFile(name)) continue;
                if (!name.startsWith(basePackagePath)) continue;

                String className = name.substring(0, name.length() - 6).replace('/', '.');

                Class<?> beanClass = classLoader.loadClass(className);

                BeanDefinition bd = reader.read(
                        beanClass,
                        beanNameGenerator.generateBeanName(beanClass, registry)
                );

                if (bd != null) beanDefinitions.add(bd);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new BeanScanException("jar 파일 스캔 중 예외가 발생했습니다.", e);
        }
    }

    private String getFullNameFormat(String currPackage, String fileName) {
        String simpleFileName = fileName.substring(0, fileName.length() - 6);
        return currPackage + "." + simpleFileName;
    }

    private boolean isRootNameFile(String fileName) {
        return !fileName.endsWith(".class") || fileName.contains("$"); //$는 inner class
    }
}
