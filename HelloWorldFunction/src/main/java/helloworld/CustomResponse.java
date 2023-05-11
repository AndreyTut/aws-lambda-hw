package helloworld;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomResponse {
    private Integer statusCode;
    private String message;
    private String error;
}
