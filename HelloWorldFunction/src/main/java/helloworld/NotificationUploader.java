package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for requests to Lambda function.
 */
public class NotificationUploader implements RequestHandler<Object, CustomResponse> {

    private AmazonSQS sqsClient;
    private AmazonSNS snsClient;

    private String topicArn;
    private String queueUrl;

    public NotificationUploader() {
        init();
    }

    @Override
    public CustomResponse handleRequest(Object o, Context context) {
        try {
            String message;
            if (checkNotificationsAndSendToTopicIfAny()) {
                message = "notifications were sent";
            } else {
                message = "No notification to send";
            }
            return CustomResponse.builder()
                    .statusCode(200)
                    .message(message)
                    .build();
        } catch (Exception e) {
            return CustomResponse.builder()
                    .statusCode(500)
                    .message("Inner server error")
                    .error(e.toString())
                    .build();
        }
    }

    private Boolean checkNotificationsAndSendToTopicIfAny() {
        ReceiveMessageRequest request = new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(10);
        List<Message> messages = new ArrayList<>();
        while (true) {
            List<Message> batch = sqsClient.receiveMessage(request).getMessages();
            if (batch.isEmpty()) {
                break;
            }
            messages.addAll(batch);
        }
        messages.forEach(this::publishToTopicAndDelete);
        return !messages.isEmpty();
    }

    private AmazonSQS amazonSQSClient() {
        return AmazonSQSClient.builder()
                .withRegion("us-east-1")
                .build();
    }

    private AmazonSNS amazonSNSClient() {
        return AmazonSNSClient.builder()
                .withRegion("us-east-1")
                .build();
    }

    private void publishToTopicAndDelete(Message message) {
        PublishRequest publishRequest = new PublishRequest()
                .withTopicArn(topicArn)
                .withMessage(message.getBody());
        snsClient.publish(publishRequest);

        DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest()
                .withQueueUrl(queueUrl)
                .withReceiptHandle(message.getReceiptHandle());
        sqsClient.deleteMessage(deleteMessageRequest);
    }

    private void init() {
        sqsClient = amazonSQSClient();
        snsClient = amazonSNSClient();
        topicArn = System.getenv("topicArn");
        queueUrl = System.getenv("queueUrl");
    }
}
