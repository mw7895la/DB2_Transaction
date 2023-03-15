package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
public class TxLevelTest {

    @Autowired
    LevelService service;

    @Test
    void orderTest(){
        service.write();
        service.read();
    }

    @TestConfiguration
    static class TxLevelTestConfig{
        @Bean
        LevelService levelService(){
            return new LevelService();
        }
    }

    @Slf4j
    @Transactional(readOnly = true)      //읽기 전용 트랜잭션
    static class LevelService {

        @Transactional(readOnly = false)        //수정해야 되니까 readOnly를 false  - 읽기와 쓰기 다 된다는 것. default가 false라 흑색으로 보이는 것.
        public void write(){
            log.info("call write");
            printTxInfo();
        }

        public void read(){         //LevelService 클래스 레벨에 @Transactional(readOnly = true) 가 있으니까 여기도 적용이 됨 그래서 생략한 것.
            log.info("call read");
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
