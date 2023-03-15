package hello.springtx.order;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class OrderServiceTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Test       //정상 승인 테스트
    void order() throws NotEnoughMoneyException {
        //given
        Order order = new Order();
        order.setUsername("정상");

        //when
        orderService.order(order);

        //then
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("완료");

        //근데 내가 테이블을 만든적이 없는데? 강의자료 참고

    }

    @Test                            //예외때문에 롤백하는 테스트.
    void runtimeException(){        //JPA의 특징, 커밋안하고 롤백하면 인서트 쿼리는 로그로 안남김.
        Order order = new Order();
        order.setUsername("예외");

        //when
        Assertions.assertThatThrownBy(() -> orderService.order(order))
                .isInstanceOf(RuntimeException.class);


        //then

        //위에서 예외가 터졌으니 롤백되니까 디비에 반영되지 않고, 조회했을때 데이터가 없어야돼
        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        assertThat(orderOptional.isEmpty()).isTrue();
    }

    @Test
    void bizException(){
        Order order = new Order();
        order.setUsername("잔고부족");

        //when
        try {
            orderService.order(order);
        } catch (NotEnoughMoneyException e) {
            log.info("고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내");
        }


        //then  // 서비스 상황예외니까 커밋되어야함.

        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("대기");
    }
}