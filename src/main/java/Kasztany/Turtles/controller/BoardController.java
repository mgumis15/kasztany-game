package Kasztany.Turtles.controller;

import Kasztany.Turtles.gui.ImageBoxElement;
import Kasztany.Turtles.model.*;
import Kasztany.Turtles.settings.GlobalSettings;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


@Controller
public class BoardController {
    private Board board;
    private final ImageBoxElement fruitBoxElement = new ImageBoxElement("peach.png");
    private final ImageBoxElement finishBoxElement = new ImageBoxElement("finish.png");
    @FXML
    private VBox pane;
    @FXML
    private HBox playersBox;
    @FXML
    private GridPane boardGrid;
    @FXML
    private Button moveButton;

    private final ArrayList<Field> possibleFields = new ArrayList<>();
    private Turtle choosedTurtle = null;
    private Field choosedField = null;
    private Field endField = null;

    public BoardController() throws FileNotFoundException {
    }

    @FXML
    public void initialize() {
        pane.setPrefSize(GlobalSettings.BOARD_WIDTH, GlobalSettings.BOARD_HEIGHT);

        playersBox.setPrefSize(GlobalSettings.GRID_WIDTH, GlobalSettings.BOARD_HEIGHT - GlobalSettings.GRID_HEIGHT);
        playersBox.setAlignment(Pos.CENTER);
        boardGrid.setPrefSize(GlobalSettings.GRID_WIDTH, GlobalSettings.GRID_HEIGHT);
    }

    @FXML
    public void receiveData(Board receivedBoard) {
        board = receivedBoard;
        endField = board.getLastField();
        playersBox.setSpacing(GlobalSettings.GRID_WIDTH / board.getTurtles().size() - (GlobalSettings.BOARD_HEIGHT - GlobalSettings.GRID_HEIGHT));
        board.getTurtles().forEach(turtle -> {
            Text turtleText = new Text(turtle.getName());
            HBox turtleIcon = drawTurtle(GlobalSettings.HEADER_TURTLE_SIZE, turtle.getColor());
            VBox turtleBox = new VBox(turtleText, turtleIcon);
            turtleBox.setOnMouseClicked((e) -> turtleClick(turtle));
            turtleBox.setOnMouseEntered((e) -> turtleIcon.setCursor(Cursor.HAND));
            playersBox.getChildren().add(turtleBox);
        });

        Vector2d maxVector = board.getMaxVector();
        double prefSize = GlobalSettings.GRID_WIDTH / (maxVector.x() + 1);
        int fruitSize = (int) prefSize / 2;
        fruitBoxElement.setSize(fruitSize);

        for (int x = 0; x <= maxVector.x(); x++) {
            boardGrid.getColumnConstraints().add(new ColumnConstraints(50, prefSize, 200));
        }
        for (int y = 0; y <= maxVector.y(); y++) {
            boardGrid.getRowConstraints().add(new RowConstraints(50, prefSize, 200));
        }
        drawBoard();
    }

    @FXML
    public void handleMoveClick(ActionEvent event) throws IOException {
        if (choosedTurtle != null && choosedField != null) {
            Field nextTurtleField = choosedField;
            choosedTurtle.move(nextTurtleField);
            if (nextTurtleField.getFruit().isPresent()) {
                choosedTurtle.eat(nextTurtleField.getFruit().get());
                nextTurtleField.deleteFruit();
            }
            if (board.isGameEnd()) {
                showEndView(event);
            }
            drawBoard();
            moveButton.setDisable(true);
            choosedTurtle = null;
            setMoveButtonColor("454242");
        }
    }

    private void turtleClick(Turtle turtle) {
        if (!board.isGameEnd()) {
            Field field = turtle.getCurrentField();
            for (Field possibleField : possibleFields) {
                Node boardField = boardGrid.lookup("#" + possibleField.getId());
                boardField.getStyleClass().clear();
                boardField.getStyleClass().add("regularField");

            }
            if (choosedField != null) {
                Node lastChoosenField = boardGrid.lookup("#" + choosedField.getId());
                lastChoosenField.getStyleClass().clear();
                lastChoosenField.getStyleClass().add("regularField");
            }
            choosedField = null;
            possibleFields.clear();
            moveButton.setDisable(true);
            highlightPossibleFieldsToMove(field, 1,true);
            highlightPossibleFieldsToMove(field, 1,false);
            for (Field possibleField : possibleFields) {
                Node boardField = boardGrid.lookup("#" + possibleField.getId());
                boardField.getStyleClass().clear();
                boardField.getStyleClass().add("possibleField");

                boardField.setOnMouseClicked((e) -> {
                    if (choosedField != null) {
                        Node lastChoosenField = boardGrid.lookup("#" + choosedField.getId());
                        lastChoosenField.getStyleClass().clear();
                        lastChoosenField.getStyleClass().add("regularField");
                    }
                    boardField.getStyleClass().clear();
                    boardField.getStyleClass().add("choosedField");
                    choosedField = possibleField;
                    moveButton.setDisable(false);
                });
            }

            choosedTurtle = turtle;
            setMoveButtonColor(turtle.getColor());
        }
    }

    private void highlightPossibleFieldsToMove(Field previousField, int steps,boolean moveForward) {
        steps--;
        ArrayList<Direction> possibleDirecctions;
        if(moveForward)
            possibleDirecctions=previousField.getPossibleForwardDirections();
        else
            possibleDirecctions=previousField.getPossibleBackwardDirections();
        for (Direction direction : possibleDirecctions) {
            Field nextField = board.getFieldForTurtleMove(previousField.getPosition(), direction);
            if (nextField != null) {
                if (steps == 0) {
                    possibleFields.add(nextField);
                } else {
                    highlightPossibleFieldsToMove(nextField, steps, moveForward);
                }
            }
        }
    }

    private HBox drawTurtle(double size, String color) {
        HBox shell = new HBox();
        shell.setMinSize(size / 2, size / 2);
        shell.setStyle("-fx-background-color: #" + color);
        HBox headBox = new HBox();
        headBox.setMinSize(size / 2, size / 2);
        headBox.setAlignment(Pos.BOTTOM_LEFT);

        HBox head = new HBox();
        head.setMaxSize(size / 2, size / 3);
        head.setMinSize(size / 2, size / 3);
        head.getStyleClass().add("turtleHead");
        head.setStyle(
                "-fx-padding:" + size / 20 + ";\n"
        );
        head.setAlignment(Pos.TOP_RIGHT);
        headBox.getChildren().add(head);
        HBox eye = new HBox();
        eye.setMaxSize(4, 4);
        eye.setMinSize(4, 4);
        eye.getStyleClass().add("turtleEye");
        head.getChildren().add(eye);

        HBox turtle = new HBox(shell, headBox);
        turtle.setPrefSize(size, size / 2);
        return turtle;
    }

    private void drawBoard() {
        Platform.runLater(() -> {
            boardGrid.getChildren().clear();
            Vector2d maxVector = board.getMaxVector();
            double size = Math.max(GlobalSettings.GRID_WIDTH / (maxVector.x() + 1), GlobalSettings.MIN_TURTLE_SIZE);
            for (Field field : board.getNeighbourhood().getFields()) {
                GridPane fieldBox = new GridPane();
                fieldBox.setId(field.getId());
                fieldBox.setMinSize(GlobalSettings.MIN_TURTLE_SIZE, GlobalSettings.MIN_TURTLE_SIZE);
                fieldBox.getStyleClass().add("field");
                if (field.getBottomTurtle().isPresent()) {
                    Turtle turtle = field.getBottomTurtle().get();
                    ArrayList<Turtle> turtlesOnField = new ArrayList<>();
                    turtlesOnField.add(turtle);
                    while (turtle.getTurtleOnBack().isPresent()) {
                        turtle = turtle.getTurtleOnBack().get();
                        turtlesOnField.add(turtle);
                    }
                    drawTurtlesInField(size / (turtlesOnField.size() + 1), fieldBox, turtlesOnField);
                } else if (field.getFruit().isPresent()) {
                    fieldBox.add(fruitBoxElement.getImage(), 0, 0);
                }
                if (field == endField) {
                    fieldBox.getStyleClass().add("endField");
                    finishBoxElement.setSize((int) fieldBox.getMinWidth());
                    fieldBox.add(finishBoxElement.getImage(), 0, 0);
                }
                fieldBox.setAlignment(Pos.CENTER);
                boardGrid.add(fieldBox, field.getPosition().x(), maxVector.y() - field.getPosition().y());
            }
            boardGrid.setAlignment(Pos.CENTER);
        });
    }

    private void setMoveButtonColor(String color) {
        moveButton.setStyle(
                "-fx-border-color: #" + color + ";"
        );
    }

    private void drawTurtlesInField(double size, GridPane field, ArrayList<Turtle> turtlesOnField) {
        for (int i = 0; i < turtlesOnField.size(); i++) {
            field.add(drawTurtle(size, turtlesOnField.get(i).getColor()), 0, turtlesOnField.size() - i - 1);
        }
    }

    private void showEndView(ActionEvent event) throws IOException {
        Turtle winner = board.findWinner();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/EndGame.fxml"));
        Parent root = loader.load();
        EndGame endGame = loader.getController();
        endGame.reveiceData(winner);
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("End Game");
        stage.setScene(scene);
        GlobalSettings.setScreenInTheMiddle(stage);
    }
}
