package hello.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class RollbackTest {

    @Autowired
    RollbackService service;

    @TestConfiguration
    static class RollbackTestConfig{
        @Bean
        RollbackService rollbackService(){
            return new RollbackService();
        }
    }

    @Test
    void runtimeException(){
        Assertions.assertThatThrownBy(() -> service.runtimeException())
                .isInstanceOf(RuntimeException.class);
        //service.runtimeException();
    }

    @Test
    void checkedException(){
        Assertions.assertThatThrownBy(() -> service.checkedException())
                .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackFor(){
        Assertions.assertThatThrownBy(() -> service.rollbackFor())
                .isInstanceOf(MyException.class);
    }

    
    @Slf4j
    static class RollbackService{
        //런 타임 예외 발생 : 롤백
        @Transactional
        public void runtimeException(){
            log.info("call runtimeException");
            throw new RuntimeException();

            // Test runtimeException()을 호출 하면, 트랜잭션을 시작했는데 커밋된건지 롤백된건지 로그로 확인이 안되네. Completing transaction for... 만 남긴다 이건 롤백이나 커밋하면 남기는건데??
            // 강의자료의 레벨로깅들을 등록해라.
            //트랜잭션의 이름은 클래스.메소드로 정해진다. RollbackTest$RollbackService.runtimeException
        }
        
        //체크 예외 발생 : 커밋
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }


        //체크 예외 발생해서 rollbackFor 지정(추가지정) : 롤백
        @Transactional(rollbackFor = MyException.class)     //체크예외지만 나는 롤백 할거야.
        public void rollbackFor() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }
    }

    static class MyException extends Exception{

    }
}
