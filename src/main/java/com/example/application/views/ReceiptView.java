package com.example.application.views;

import com.example.application.service.DeepSeekService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.core.parameters.P;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Route("")
@PageTitle("Receipt")
@Menu(title = "Receipt", order = 1)
public class ReceiptView extends VerticalLayout {
    public record LineItem(String name, int quantity, BigDecimal price) {
    }

    public record Receipt(String merchant, BigDecimal total, List<LineItem> lineItems) {
    }
    private Paragraph photoName;
    private File targetFile;
    private Component previousPhoto;

    public ReceiptView(ChatClient.Builder builder) {
        var client = builder.build();

        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/*");
        upload.addSucceededListener(e -> {
            var receipt = client.prompt()
                .user(userMessage -> userMessage
                    .text("Please read the attached receipt and return the value in provided format")
                    .media(
                        MimeTypeUtils.parseMimeType(e.getMIMEType()),
                        new InputStreamResource(buffer.getInputStream())
                    )
                )
                .call()
                .entity(Receipt.class);

            showReceipt(receipt);
            upload.clearFileList();
        });
        Text instructions = new Text("Upload an image of a receipt. The AI will extract the details and show them below.");
        add(instructions, upload);
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
    public static void analyzeImage(MemoryBuffer buffer) {

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

}
