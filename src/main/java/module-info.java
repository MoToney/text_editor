module text_editor.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens texteditor.app to javafx.fxml;
    exports texteditor.app;

}