package hello.springtx.apply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV2Test {

    @Autowired
    CallService callService;        //internal() 에 @Transactional이 있기 때문에 프록시가 주입이 된다.

    @Test
    void printProxy(){
        log.info("callService class={}", callService.getClass());       //프록시가 적용이 됬냐?'
    }


    @Test
    void externalCallV2(){
        callService.external();     //뭐지????? 내부에 interanl()이 있는데 internal()에 트랜잭션이 적용되지 않았다.
    }

    @TestConfiguration
    static class InternalCallV1TestConfig{
        @Bean
        CallService callService(){
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService(){
            return new InternalService();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService{
        /** 이제 CallService가 InternalService를 주입 받았다.
         *  InternalService는 빈으로 등록될떄 @Transactionl 어노테이션이 있으니 InternalService를 상속받은 프록시가 빈으로 등록된다.
         *
        */
        private final InternalService internalService;

        public void external(){
            //외부에서 호출하는 메서드
            log.info("call external");
            printTxInfo();
            internalService.internal();
        }

/*        @Transactional
        public void internal(){
            log.info("call internal");
            printTxInfo();
        }*/

        private void printTxInfo(){
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);

            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();        // readOnly 옵션이 뭐로 적용되어있냐 확인용.
            log.info("tx readOnly={}", readOnly);

        }
    }

    static class InternalService{
        @Transactional
        public void internal(){
            log.info("call internal");
            printTxInfo();
        }
        private void printTxInfo(){
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);

            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();        // readOnly 옵션이 뭐로 적용되어있냐 확인용.
            log.info("tx readOnly={}", readOnly);

        }
    }

}
