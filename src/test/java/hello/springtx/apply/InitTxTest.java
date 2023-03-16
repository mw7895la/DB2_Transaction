package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;

@SpringBootTest
public class InitTxTest {

    @Autowired
    Hello hello;

    @Test
    void go(){
        //초기화 코드는 스프링이 초기화 시점에 호출한다.
    }
    //go() 실행하면 class Hello 안에 코드들 실행된다 왜??
    //스프링 컨테이너가 스프링 빈에 Hello 등록하겠지 그 타이밍에 등록하고 나서 초기화 메서드를 자동으로 호출해 준다. 직접 호출하면 트랜잭션 적용 되지..
    // go를 호출하면  hello.springtx.apply.InitTxTest$Hello    : hello init @PostConstruct tx active =false 가 찍힌다.  @PostConstruct와 @Transactional은 같이 사용할 수 없다.
    // 초기화 코드가 먼저 실행되고나서  그 다음에 트랜잭션 AOP가 적용되기 때문이다.
    //대책은?  초기화보다도 더 이후에 스프링이 완전히 컨테이너 다 만들고 AOP 다 만든 다음에 @Transactional 붙은 initV1()을 호출하게 하는 것이다.  -> @EventListener(ApplicationReadyEvent.class)를 사용하자
    @TestConfiguration
    static class InitTxTestConfig{

        @Bean
        Hello hello(){
            return new Hello();
        }
    }

    @Slf4j
    static class Hello{

        //PostConstruct가 호출되는 시점은 자기 빈을 초기화 하는 시점으로 아주 빠르게 일어납니다.
        //따라서 트랜잭션 프록시가 아직 적용되지 않은 상태에서 이런 호출이 발생하게 됩니다.
        //https://www.inflearn.com/questions/560186/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%B9%88%EC%9D%98-%EC%9D%B4%EB%B2%A4%ED%8A%B8-%EB%9D%BC%EC%9D%B4%ED%94%84-%EC%82%AC%EC%9D%B4%ED%81%B4-%EA%B4%80%EB%A0%A8-%EC%A7%88%EB%AC%B8
        //참고
        @PostConstruct
        @Transactional
        public void initV1(){
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("hello init @PostConstruct tx active ={}", isActive);
        }

        @EventListener(ApplicationReadyEvent.class)     //스프링 컨테이너가 완전히 다 뜨고 나서 호출 , 스프링 AOP 트랜잭션 다~ 적용해서 완성이 되고 나서 호출된다.
        @Transactional
        public void initV2() {
            //Started InitTxTest in 2.28 seconds (JVM running for 3.563) 은 스프링 컨테이너가 완료되서 뜬 것을 알려주는 로그
            //Getting transaction for [hello.springtx.apply.InitTxTest$Hello.initV2]  프록시 부분.
            //Test worker] hello.springtx.apply.InitTxTest$Hello    : hello init ApplicationReadyEvent tx active =true 가 찍힐 것.
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("hello init ApplicationReadyEvent tx active ={}", isActive);
        }
    }
}
