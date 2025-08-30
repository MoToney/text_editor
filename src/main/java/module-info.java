module text_editor.app {
    requires javafx.controls;
    requires javafx.fxml;


    opens texteditor.app to javafx.fxml;
    exports texteditor.app;
}