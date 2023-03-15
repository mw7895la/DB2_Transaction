package hello.springtx.order;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "orders")     //데이터베이스 테이블은 orders라는 테이블과 매핑해서 사용할 것. 우린 지금 메모리DB를 쓸것이다.  JPA가 자동으로 테이블을 만드는 모드가 있다.
@Getter
@Setter
public class Order {

    //JPA를 쓰자

    @Id
    @GeneratedValue
    private Long id;

    private String username;        //정상 , 에외, 잔고부족

    private String payStatus;       //대기, 완료,

}
