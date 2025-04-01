package com.example.application.views;
import com.example.application.service.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.core.parameters.P;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

@Route("Shoes")
@PageTitle("Shoes")
@Menu(title = "Shoes", order = 1)
public class ShoesView extends VerticalLayout {
    public record LineItem(String brand, String name, int quantity, BigDecimal price) {
    }

    public record Receipt(String merchant, String address, BigDecimal total, List<LineItem> lineItems) {
    }

    private Paragraph photoName;
    private File targetFile;

    private Component previousPhoto;

    public ShoesView(ChatClient.Builder builder) {
        var client = builder.build();
        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        var output = new Div();

        Text instructions = new Text("Upload an image of a receipt. The AI will extract the details and show them below.");
        add(instructions, upload, output);

        upload.setAcceptedFileTypes("image/*");
        upload.addSucceededListener(e -> {
            InputStream inputStream = buffer.getInputStream();
            try {
                byte[] bytes = IOUtils.toByteArray(inputStream);
                    Component component = createComponent(e.getMIMEType(), e.getFileName(), bytes);
                    try {
                        File targetFile = new File(getAppPath() + e.getFileName());
                        FileUtils.copyInputStreamToFile(new ByteArrayInputStream(bytes), targetFile);
                        showOutput(e.getFileName(), component, output);
                        analyzeImage(targetFile);
                        Paragraph out = new Paragraph();
                        var receipt = client.prompt()
                                .user(userMessage -> userMessage
                                        .text("Please read the attached shoes and return the value in provided format")
                                        .media(
                                                MimeTypeUtils.parseMimeType(e.getMIMEType()),
                                                new InputStreamResource(buffer.getInputStream())
                                        )
                                )
                                .call()
                                .entity(ShoesView.Receipt.class);

                        showReceipt(receipt);
                        upload.clearFileList();


                    }catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }});

    }

    @Autowired
    private Environment env;
    public static String getShowImage(String name) throws Exception {
        // Assuming the image is in the same directory as the Java file
        Path imagePath = Paths.get("src/" + name);
        byte[] imageBuffer = Files.readAllBytes(imagePath);
        String base64Image = Base64.getEncoder().encodeToString(imageBuffer);
        return "data:image/jpeg;base64," + base64Image;
    }
    public String getAppPath() {
        return env.getProperty("spring.servlet.multipart.location");
    }


    private Component createComponent(String mimeType, String fileName, byte[] bytes) {
        if (mimeType.startsWith("image")) {
            Image image = new Image();
            image.setMaxWidth("100%");
            image.setSrc(new StreamResource(fileName, () -> new ByteArrayInputStream(bytes)));
            return image;
        } else {
            Div content = new Div();
            String text = String.format(
                    "Mime type: '%s'\nSHA-256 hash: '%s'",
                    mimeType,
                    generateSHA256Hash(bytes)
            );
            content.setText(text);
            return content;
        }
    }

    private String generateSHA256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    private void showOutput(String text, Component content, HasComponents outputContainer) {
        if (photoName != null) {
            outputContainer.remove(photoName);
        }
        if (previousPhoto != null) {
            outputContainer.remove(previousPhoto);
        }
        photoName = new Paragraph(text);
        outputContainer.add(photoName);
        previousPhoto = content;
        outputContainer.add(previousPhoto);
    }

    private void showReceipt(Receipt receipt) {
        var items = new Grid<>(LineItem.class);
        items.setItems(receipt.lineItems());

        add(
                new H3("Receipt details"),
                new Paragraph("Merchant: " + receipt.merchant()),
                new Paragraph("Total: " + receipt.total()),
                items
        );
    }


    public void analyzeImage(File name) {
        try {
            if (targetFile == null || !targetFile.exists()) {
                Notification.show("Please upload an image first");
                return;
            }

            try {
                byte[] imageBytes = Files.readAllBytes(targetFile.toPath());
                String nameImg = targetFile.getName();
                String base64ImageData = Base64.getEncoder().encodeToString(imageBytes);
                System.out.println("OUT " + base64ImageData);
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode payload = mapper.createObjectNode();

                // Create the messages array
                ArrayNode messages = mapper.createArrayNode();
                ObjectNode message = mapper.createObjectNode();
                message.put("role", "user");

                // Create the content array
                ArrayNode content = mapper.createArrayNode();
                content.add(mapper.createObjectNode()
                        .put("type", "text")
                        .put("text", "What's in this image?"));
                content.add(mapper.createObjectNode()
                        .put("type", "image_url")
                        .set("image_url", mapper.createObjectNode()
                                .put("url", "data:image/jpeg;base64," + getShowImage(nameImg))));
                message.set("content", content);
                messages.add(message);

                // Add fields to the payload
                payload.put("model", "gpt-4o-mini");
                payload.set("messages", messages);
                payload.put("max_tokens", 300);

                // Print the payload
                System.out.println("Request Payload: " + payload.toPrettyString());

                // Create the HTTP request
                HttpPost request = new
                        HttpPost("https://api.openai.com/v1/chat/completions");
                request.setEntity(new StringEntity(payload.toString()));
                request.addHeader("Content-Type", "application/json");
                request.addHeader("Authorization", "Bearer " + "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"); //

                // Execute the request
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    try (CloseableHttpResponse response = client.execute(request)) {
                        System.out.println("Status Code: " +
                                response.getStatusLine().getStatusCode());
                        System.out.println("Response Body: " +
                                EntityUtils.toString(response.getEntity()));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

