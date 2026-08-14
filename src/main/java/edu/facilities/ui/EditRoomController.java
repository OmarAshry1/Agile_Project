package edu.facilities.ui;
import edu.facilities.model.*; import edu.facilities.service.RoomService; import javafx.fxml.FXML; import javafx.scene.control.*;
public class EditRoomController { @FXML private TextField codeField,nameField,capacityField,locationField; @FXML private ComboBox<RoomType> typeBox; @FXML private ComboBox<RoomStatus> statusBox; @FXML private Label messageLabel;
 @FXML public void initialize(){typeBox.getItems().addAll(RoomType.values());statusBox.getItems().addAll(RoomStatus.values());typeBox.setValue(RoomType.CLASSROOM);statusBox.setValue(RoomStatus.AVAILABLE);}
 @FXML private void save(){try{new RoomService().updateRoom(codeField.getText(),nameField.getText(),typeBox.getValue(),Integer.parseInt(capacityField.getText()),locationField.getText(),statusBox.getValue());messageLabel.setText("Room updated.");}catch(Exception e){messageLabel.setText(e.getMessage());}}
}
