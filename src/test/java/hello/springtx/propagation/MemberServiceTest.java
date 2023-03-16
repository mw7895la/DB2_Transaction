package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;

    /**
     * memberService        @Transactional :OFF
     * memberRepository      @Transactional :ON
     * logRepository        @Transactional :ON
     */

    @Test
    void outerTxOff_success(){
        //외부 트랜잭션 없는 경우,
        String username = "outerTxOff_success";

        memberService.joinV1(username);

        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService        @Transactional :OFF
     * memberRepository      @Transactional :ON
     * logRepository        @Transactional :ON  RuntimeException !!
     */
    @Test
    void outerTxOff_fail() {

        //Log 에서 RuntimeException 발생 !
        String username = "로그예외_outerTxOff_fail";

        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService            @Transactional :On
     * memberRepository         @Transactional :OFF
     * logRepository            @Transactional :OFF
     *
     * 강의자료 14장 부분.
     */

    @Test
    void singleTx(){
        //MemberService에만 트랜잭션을 적용.
        String username = "singleTx";

        memberService.joinV1(username);
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }


    /**
     * memberService         @Transactional :ON
     * memberRepository      @Transactional :ON
     * logRepository         @Transactional :ON
     *
     * 강의자료 18장 부분.
     */

    @Test
    void outerTxOn_success(){
        //given
        String username = "outerTxOn_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService         @Transactional :ON
     * memberRepository      @Transactional :ON
     * logRepository         @Transactional :ON  RuntimeException !!
     * 강의자료 21 page
     * 아래의 시나리오는, 먼저 joinV1을 통해 username을 전달하면, LogRepository 에서 save를 하려고 할 때, 런타임 예외가 터진다. 그럼 트랜잭션 동기화 매니저에 rollback-only 표시를 할 것이다.
     * 그리고 나서 예외를 joinV1으로 던진다. 그럼 joinV1에선 런타임예외를 받았으니 (롤백되어야 겠지.) 그럼 아래 테스트의 모든 데이터들은 롤백된다. 그래서  find하면 isEmpty()로 확인해야함.
     */

    @Test
    void outerTxOn_fail(){
        //given
        String username = "로그예외_outerTxOn_fail";

        //when

        assertThatThrownBy(() -> memberService.joinV1(username)).isInstanceOf(RuntimeException.class);

        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
        //위에서 memberService에 username 넘기고 LogRepository에서 예외가 발생하니까. 롤백됨 그래서 데이터가 없으니 isEmpty()로 하면 테스트 통과.
    }

    /**
     * memberService         @Transactional :ON
     * memberRepository      @Transactional :ON
     * logRepository         @Transactional :ON  RuntimeException !!
     * 강의자료 23 page
     * 익셉션을 memberService단에서 복구할텐데 결국 트랜잭션 롤백이다.
     * 아래는 joinV2 메소드 사용한다. (예외를 잡는)
     *
     * 일반적인 예상으로는 예외를 잡았으니, 멤버는 저장이되고 로그는 롤백되었겠지 라고생각하는데....... 아니다 !! 전체가 롤백된다.
     * 논리 트랜잭션 C 에서 예외가 발생해서 throw하면 Service로 넘어오게 된다. Service에서 예외를 잡고((잡으면 정상흐름이 된다)) 이제 커밋을 호출 하려 했더니, 트랜잭션 동기화 매니저에 rollback-only가 표시되어있다.
     * 그래서 앗, 커밋하면 안되지 하면서 물리 트랜잭션 rollback 처리해버린다. 그리고 클라이언트에게 UnexpectedRollbackException을 던진다 !
     */

    @Test
    void recoverException_fail(){
        String username = "로그예외_recoverException_fail";

        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService         @Transactional :ON
     * memberRepository      @Transactional :ON
     * logRepository         @Transactional :ON (REQUIRES_NEW 로 바꾸자) RuntimeException !!
     * 강의자료 26 page
     *
     * memberService joinV2와 memberRepository는 같은 트랜잭션이다. Log는 완전 별도의 트랜잭션 영역이다. 여기만 롤백되고 바깥에 영향 안준다.
     * 근데 롤백이 되더라도 익셉션이 터졌기 때문에  Service의 joinV2에서 익셉션을 잡고 정상흐름으로 반환하면 이 부분은 이제 커밋을 요청한다.
     * rollback-only 마크는 당연히 없겠지 그래서 커밋이 된다.
     */
    @Test
    void recoverException_success() {
        //given
        String username = "로그예외_recoverException_success";

        //when
        memberService.joinV2(username);


        //when : member는 저장되고 log는 롤백된다.
        org.junit.jupiter.api.Assertions.assertTrue(memberRepository.find(username).isPresent());
        //Optional 로 리턴되기 때문에 그 안에 값이 존재하는지 보려고 isPresent()를 썼다 존재하면 True
        org.junit.jupiter.api.Assertions.assertTrue(logRepository.find(username).isEmpty());

    }
}




