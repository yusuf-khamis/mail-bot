
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.stage.StageStyle;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.*;
import java.lang.Thread;
import java.util.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Created by Yusuf Khamis on 4/20/2017.
 */
public class HomeController extends Application {

    private static final String APPLICATION_NAME = "Lulliezy GMail Bot";
    private static final String ATTACHMENT_NAME = "YUSUF_ALI_HAMISI_MWISHEE_RENGE.pdf";
    private static final String FROM = "yusufkkhamis27@gmail.com";
    private static final String SUBJECT = "Résumé";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"),
            ".credentials/gmail-mail-bot");
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_SEND);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    StackPane root;
    RedisOperations redisOperations;
    Thread thread;
    Text status;
    ListView<String> mailList;

    @Override
    public void start(Stage primaryStage) throws Exception{
        root = new StackPane();
        redisOperations = new RedisOperations();
        thread = new Thread(new SendMails());

        status = new Text("Attachment: " + ATTACHMENT_NAME);
        status.setFill(Color.WHITE);
        status.setFont(Font.font(12));

        if(redisOperations.testRedis()) {
            status.setText("Redis is not working");
        }

        Rectangle mainBG = new Rectangle(500, 300);
        mainBG.setFill(Color.BLACK);
        mainBG.setOpacity(0.8);
        mainBG.setArcHeight(10);
        mainBG.setArcWidth(10);
        mainBG.setEffect(new BoxBlur(10, 10, 3));

        Label title = new Label("Lulliezy mail bot");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(null, FontWeight.BOLD, 15));

        Label close = new Label("X");
        close.setTextFill(Color.WHITE);
        close.setFont(Font.font(null, FontWeight.BOLD, 15));
        close.setCursor(Cursor.HAND);
        close.setOnMouseClicked((e) -> {
            Platform.exit();
        });

        ImageView action = new ImageView(getClass().getClassLoader().getResource("execute_action.png").toString());
        action.setFitHeight(17);
        action.setFitWidth(17);
        action.setSmooth(true);
        action.setCursor(Cursor.HAND);
        action.setOnMouseClicked((e) -> {
            if(redisOperations.allEmails().size() == 0) {
                status.setText("Currently no mails set");

                return;
            }

            thread.start();
        });

        VBox outerVbox = new VBox(10);
        outerVbox.setPadding(new Insets(5));
        BorderPane titlePane = new BorderPane();
        HBox controls = new HBox(30);
        HBox hBox = new HBox(20);
        VBox mails = new VBox(10);
        VBox contents = new VBox(10);

        mailList = new ListView<>();
        mailList.setPrefSize(230, 150);
        for(String item: redisOperations.allEmails()) {
            mailList.getItems().add(item);
        }
        mailList.setOnKeyPressed((e) -> {
            if(mailList.getSelectionModel().getSelectedItems().size() < 1) {

                return;
            }
            if(e.getCode() == KeyCode.DELETE) {
                for (String item: mailList.getSelectionModel().getSelectedItems()) {
                    redisOperations.removeFromList(item);
                    mailList.getItems().remove(item);
                }
            }
        });

        TextField newEmail = new TextField();
        newEmail.setPrefWidth(230);
        newEmail.setOnAction((e) -> {
            if(redisOperations.allEmails().contains(newEmail.getText().trim())) {
                status.setText("Email already exists");

                return;
            }

            redisOperations.addToList(newEmail.getText().trim());
            mailList.getItems().add(newEmail.getText().trim());
            newEmail.setText("");
        });

        TextArea message = new TextArea(redisOperations.getItem("message"));
        message.setPrefSize(240, 150);
        message.setWrapText(true);
        message.setOnKeyPressed((e) -> {
            if(e.getCode() == KeyCode.F9) {
                redisOperations.setItem("message", message.getText().trim());
            }
        });

        TextField subject = new TextField(redisOperations.getItem("subject") == null ? SUBJECT : redisOperations.getItem("subject"));
        subject.setOnAction((e) -> {
            redisOperations.setItem("subject", subject.getText().trim());
        });
        subject.setOnKeyPressed((e) -> {
            if(e.getCode() == KeyCode.F12) {
                subject.setText(SUBJECT);
            }
        });

        controls.getChildren().addAll(action, close);
        titlePane.setLeft(title);
        titlePane.setRight(controls);
        mails.getChildren().addAll(mailList, newEmail);
        contents.getChildren().addAll(subject, message);
        hBox.getChildren().addAll(mails, contents);
        outerVbox.getChildren().addAll(titlePane, hBox, status);
        root.getChildren().addAll(mainBG, outerVbox);

        Scene scene = new Scene(root, 500, 225);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("styles.css").toExternalForm());

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    protected void sendMail(String to, String subject, String message) throws MessagingException, IOException {
        File file = new File(getClass().getClassLoader().getResource(ATTACHMENT_NAME).getFile());
        if(!file.exists()) {
            file = new File(ATTACHMENT_NAME);
        }

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(FROM));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(message, "text/plain");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(file);
        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(file.getName());

        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        email.writeTo(baos);
        String encodedEmail = Base64.encodeBase64URLSafeString(baos.toByteArray());
        Message myMessage = new Message();
        myMessage.setRaw(encodedEmail);

        myMessage = getGmailService().users().messages().send("me", myMessage).execute();
//        ((Label) root.getChildren().get(0)).setText(myMessage.getLabelIds().get(0));
//        System.out.println(myMessage.getLabelIds().get(0));
    }

    protected Credential authorize() throws IOException {
        InputStream in = HomeController.class.getResourceAsStream("mail_bot.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType(null)
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    protected Gmail getGmailService() throws IOException {
        Credential credential = authorize();

        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) {
        launch(args);
    }

    class SendMails implements Runnable {
        String currentItem = "";
        public void run() {
            Platform.runLater(() -> {
                status.setText("Sending emails...");
            });

            Set<String> emails = redisOperations.allEmails();
            try{
                for (String item: emails) {
                    Platform.runLater(() -> {
                        if(mailList.getItems().contains(item)) {
                            mailList.getSelectionModel().select(item);
                            mailList.scrollTo(item);
                        }
                    });
                    currentItem = item;
                    sendMail(item, redisOperations.getItem("subject"), redisOperations.getItem("message"));
                }

                Platform.runLater(() -> {
                    status.setText("Mails sent successfully!");
                });
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                Platform.runLater(() -> {
                    status.setText(currentItem + ": " + ex.getMessage());
                });
            }
        }
    }
}
