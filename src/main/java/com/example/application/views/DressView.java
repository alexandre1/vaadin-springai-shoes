
package com.example.application.views;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.MimeTypeUtils;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Route("Dress")
@PageTitle("Dress")
@Menu(title = "Dress", order = 2)
public class DressView extends VerticalLayout {
    public record LineItem(String website, String email, String merchant, String brand, String name, BigDecimal price) {
    }

    public record Receipt(String address, BigDecimal total, List<LineItem> lineItems) {
    }

    private Paragraph photoName;
    private File targetFile;

    private Component previousPhoto;

    public DressView(ChatClient.Builder builder) {
        var client = builder.build();
        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        var output = new Div();

        Text instructions = new Text("Upload an image of a dress. The AI will extract the details and show them below.");
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

                    Paragraph out = new Paragraph();
                    var receipt = client.prompt()
                            .user(userMessage -> userMessage
                                    .text("Please read the attached dress and return the value in provided format")
                                    .media(
                                            MimeTypeUtils.parseMimeType(e.getMIMEType()),
                                            new InputStreamResource(buffer.getInputStream())
                                    )
                            )
                            .call()
                            .entity(DressView.Receipt.class);

                    showReceipt(receipt);
                    //showReceipt(receipt);
                    callForShop(builder, buffer);
                    upload.clearFileList();
                    //  callForShop (builder, buffer, receipt.lineItems().get(0).brand, receipt.lineItems.get(0).name);

                }catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }});

    }

    public void callForShop (ChatClient.Builder builder, MemoryBuffer buffer) {
        var client = builder.build();
        var receiptShoes = client.prompt()
                .user(userMessage -> userMessage
                        .text("Please read the attached dress and return the value in provided format")
                        .media(
                                MimeTypeUtils.parseMimeType(MimeTypeUtils.IMAGE_JPEG.toString()),
                                new InputStreamResource(buffer.getInputStream())
                        )
                )
                .call()
                .entity(DressView.Receipt.class);


        var receipt = client.prompt()
                .user(userMessage -> userMessage
                        .text("Please give us the first 100 shops where these dress are sold and provide an url to the store and a working email" + receiptShoes.lineItems.get(0).name + " name :" + receiptShoes.lineItems.get(0).brand)
                        .media(
                                MimeTypeUtils.parseMimeType(MimeTypeUtils.IMAGE_JPEG.toString()),
                                new InputStreamResource(buffer.getInputStream())
                        )
                )
                .call()
                .entity(DressView.Receipt.class);

        var items = new Grid<>(LineItem.class);
        items.setItems(receipt.lineItems());
        Editor<LineItem> editor = items.getEditor();
        Grid.Column<LineItem> editColumn = items.addComponentColumn(shoes -> {
            com.vaadin.flow.component.button.Button editButton = new com.vaadin.flow.component.button.Button("Vied detailss");
            editButton.addClickListener(e -> {
                if (editor.isOpen())
                    editor.cancel();
                //items.getEditor().editItem(shoes);
            });
            return editButton;
        }).setWidth("80px");
        add(items);



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
        add(items);
    }
}


