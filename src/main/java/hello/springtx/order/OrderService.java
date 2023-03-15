package hello.springtx.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;


    //JPA는 트랜잭션 커밋 시점에 Order 데이터를 DB에 반영한다.
    @Transactional
    public void order(Order order) throws NotEnoughMoneyException {
        log.info("Order 호출");

        orderRepository.save(order);

        log.info("결제 프로세스 진입");

        if (order.getUsername().equals("예외")) {
            //여기선 그냥 단순하게 하기위해 사용자 이름이 예외면 시스템 예외 발생
            log.info("시스템 예외 발생");
            throw new RuntimeException("시스템 예외");
        } else if (order.getUsername().equals("잔고부족")) {
            //이러면 비지니스 예외.
            log.info("잔고 부족 비즈니스 예외 발생");
            order.setPayStatus("대기");
            throw new NotEnoughMoneyException("잔고가 부족합니다");
            //우리 잔고부족이면 예외가 터지는데 하지만, 커밋을 하길 기대
            //이거 체크 예외기 때문에 던져라

        }else{
            //정상 승인
            log.info("정상 승인");
            order.setPayStatus("완료");

        }

        log.info("결제 프로세스 완료");
    }
}
