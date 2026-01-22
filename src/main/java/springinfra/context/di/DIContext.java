package springinfra.context.di;

import static util.logger.Logger.log;

import java.util.logging.Logger;

/**
 * 의존 주입의 역할을 맡는 컨텍스트이다.
 * - refresh()를 통해 빈들을 등록한다.
 *
 * 1. prepareRefresh()
 * : 준비
 *
 * 2. registerInfrastructureProcessors()
 * : BFPP, BDRPP 빈들을 먼저 싱글톤으로 등록
 *
 * 3. invokeBeanFactoryPostProcessors()
 * : BFPP, BDRPP를 통해 빈 정의를 확장/수정/추가
 *
 * 4. registerBeanPostProcessors()
 * : BPP를 싱글톤으로 등록
 *
 * 5. finishBeanFactoryInitialization()
 * : 싱글톤 빈 인스턴스들 생성/주입/초기화
 *
 * 6. finishRefresh()
 * : 마무리
 */
public class DIContext {

    public void refresh() {
        prepareRefresh();

        registerInfrastructureProcessors();

        invokeBeanFactoryPostProcessors();

        registerBeanPostProcessors();

        finishBeanFactoryInitialization();

        finishRefresh();
    }

    private void prepareRefresh() {
        log("DIContext 준비 시작");
    }

    private void registerInfrastructureProcessors() {

    }

    private void invokeBeanFactoryPostProcessors() {

    }

    private void registerBeanPostProcessors() {

    }

    private void finishBeanFactoryInitialization() {

    }

    private void finishRefresh() {

    }
}
