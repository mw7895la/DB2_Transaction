package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config{
        @Bean           //원래 스프링이 자동 등록해주는데 그냥 내가 직접 등록했다.
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

    }

    @Test
    void commit(){
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());
        //DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT ....

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);

        log.info("트랜잭션 커밋 완료");

    }

    @Test
    void rollback(){
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());
        //DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT ....

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);

        log.info("트랜잭션 롤백 완료");

    }

    //트랜잭션 1이 끝나고 나서 2가 수행되는 시나리오
    @Test
    void double_commit() {
        log.info("트랜잭션 1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("트랜잭션 1 커밋");
        txManager.commit(tx1);


        log.info("트랜잭션 2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("트랜잭션 2 커밋");
        txManager.commit(tx2);

        /**
         * 우리가 데이터소스를 만들지 않고 스프링이 등록한것을 사용했기 때문에 히카리를 사용했고 히카리는 커넥션풀을 이용한다.
         * 커넥션 풀이 아니었으면, 트랜잭션 1은 conn0 을 쓰고 트랜잭션 2는 conn1 을 썼을 것.
         *
         * 히카리가, 커넥션 조회하면  프록시 커넥션 객체를 생성해서 그 안에 실제 물리 커넥션을 담아서 반환한다.
         *
         * Acquired Connection [HikariProxyConnection@1632141948 wrapping conn0  - @1632141948 프록시 커넥션 객체의 주소   ,  wrapping conn0 내부의 실제 물리 커넥션 conn0
         *
         * Acquired Connection [HikariProxyConnection@1436969919 wrapping conn0   - @1436969919 프록시 커넥션 객체의 주소   ,  wrapping conn0 내부의 실제 물리 커넥션 conn0
         *
         * 커넥션 풀에 반납되고 새로 조회해서 얻은 것이다.
         */
    }

    //커밋하고 롤백하는 시나리오
    @Test
    void double_commit_rollback() {
        log.info("트랜잭션 1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("트랜잭션 1 커밋");
        txManager.commit(tx1);


        log.info("트랜잭션 2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("트랜잭션 2 롤백");
        txManager.rollback(tx2);
    }

    //외부 트랜잭션 수행 중인데 내부 트랜잭션이 시작 그래서 전체가 커밋되는 상황
    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        //트랜잭션 매니저는 트랜잭션을 생성한 결과를 TransactionStatus 에 담아서 반환  , 여기선 outer 이걸로 신규 트랜잭션인지 여부 확인 가능.
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("outer.isNewTransaction() ={}", outer.isNewTransaction()); //처음 수행된 트랜잭션이냐? 물어보는 것. 여기는 true일것.

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionDefinition());
        //지금 여기 까지는, 외부 트랜잭션이 시작되고 있는데  내부에서 트랜잭션 하나가 더 시작되는 것이다.

        log.info("inner.isNewTransaction() ={}", inner.isNewTransaction()); //여기는 false가 된다.

        log.info("내부 트랜잭션 커밋");     //내부 먼저 커밋or롤백
        txManager.commit(inner);

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }
    /**
     * Switching JDBC Connection [HikariProxyConnection@1943867171 wrapping conn0] to manual commit   -  set autocommit false로 한다는 것이다.
     *
     * log.info("내부 트랜잭션 커밋");
     * txManager.commit(inner);  이 부분에서 커밋한다는 로그가 찍혀야 하는데  "내부 트랜잭션 커밋" 로그 출력후 내부 트랜잭션의 commit에 대한 로그가 없고 바로 "외부 트랜잭션 커밋"이 찍힌다.
     * 내부 트랜잭션은 외부 트랜잭션에 참여한 것이다  그래서 물리 트랜잭션에 대해서 작업을 전혀 하지않는다. 그래서 commit(inner)를 무시하고 넘어가는 것.
     *
     * 스프링은 이렇게 여러 트랜잭션이 함께 사용되는 경우, 처음 트랜잭션을 시작한 외부 트랜잭션이 실제 물리 트랜잭션을 관리하도록 한다.
     *
     * 트랜잭션은  같은 커넥션을 사용한다는 것이 중요하다 !  외부 트랜잭션이 처음에 트랜잭션 시작시 생성한 커넥션을  내부 트랜잭션도 같이 사용하는 것.
     */


    //외부 시작 하고 내부 시작후 내부는 커밋했는데  외부에서 롤백 = 전체 롤백이 된다.
    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionDefinition());
        //예를 들어 DB INSERT A 하고,

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionDefinition());
        //지금 여기 까지는, 외부 트랜잭션이 시작되고 있는데  내부에서 트랜잭션 하나가 더 시작되는 것이다.
        log.info("내부 트랜잭션 커밋");
        //DB INSERT B 하면,

        txManager.commit(inner);        //물리 커넥션에 대해서 커밋한게 아니다 !  그래서 아무일도 일어나지 않는다.

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);      //이렇게 되면, A, B 다 롤백 되는 것.
    }

    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionDefinition());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionDefinition());
        //지금 여기 까지는, 외부 트랜잭션이 시작되고 있는데  내부에서 트랜잭션 하나가 더 시작되는 것이다.
        log.info("내부 트랜잭션 롤백");

        txManager.rollback(inner);
        //롤백 하고 나면,  o.s.j.d.DataSourceTransactionManager     : Participating transaction failed - marking existing transaction as rollback-only
        //내부 트랜잭션에서 롤백할 때 "나는 롤백만 해야돼"라고 외부 트랜잭션에다가 rollback-only를 표시 한다는 뜻.


        log.info("외부 트랜잭션 커밋");

        //o.s.j.d.DataSourceTransactionManager     : Global transaction is marked as rollback-only but transactional code requested commit
        //내가 원래대로 최종 커밋을 하려는데 어디선가 롤백이라는 걸 표시했다. 그래서 나는 Rollback을 할거야.

        Assertions.assertThatThrownBy(() -> txManager.commit(outer))
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    //외부 트랜잭션과 , 내부 트랜잭션을 완전히 분리해서 사용하는 방법  ( 내부 트랜잭션에 옵션을 넣어준다 PROPAGATION_REQUIRES_NEW )
    @Test
    void inner_rollback_requires_new(){
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("outer.isNewTransaction() ={}", outer.isNewTransaction());     //true

        log.info("내부 트랜잭션 시작");
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);      //항상 새로운 트랜잭션 만든다.
        //기존에 트랜잭션 있으면 참여하는 것. 이게 REQUIRES고    , REQUIRES_NEW는 뭐냐면  기존 트랜잭션은 무시하고 새로운 트랜잭션(신규)트랜잭션을 만드는 것.
        TransactionStatus inner = txManager.getTransaction(definition);         //인자로 옵션이 설정된 definition을 넣어줌.
        log.info("inner.isNewTransaction() ={}", inner.isNewTransaction());     //true

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);      //이러면 inner 로직 부분만 롤백이 된다 밖에 것 손대지 않는다.

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }
    /**
     *  분리하면 각각의 트랜잭션은 서로 다른 커넥션을 사용하는 것을 볼 수 있다.   -외부 커넥션 : conn0  -내부 커넥션 : conn1
     *  옵션으로 인해 완전히 새로운 물리 트랜잭션으로 동작하는 내부 트랜잭션이 수행하는 동안  외부 트랜잭션의 커넥션인 conn0은 잠시 트랜잭션 매니저 동기화에서 보류되고 conn1이 쓰인다.
     *  내부 트랜잭션이 완료되면 (커밋이나 롤백) 다시 conn0을 가지고 있는 외부 트랜잭션이 동작한다.
     *
     *  내부 트랜잭션 시작 시
     *  Suspending current transaction, creating new transaction with name [null] 라는 로그를 볼 수 있다.
     *  외부 트랜잭션을 잠시 미뤄두고 새로운 트랜잭션인 내부 트랜잭션을 수행 하고 있다.
     *  그리고 내부 트랜잭션의 수행이 다 끝나면
     *   Resuming suspended transaction after completion of inner transaction 으로 외부 트랜잭션을 이어서 시작한다.
     *
     *   ★★★트랜잭션은 순서대로 시작하고,  역순으로 종료해라★★★
     */
}





