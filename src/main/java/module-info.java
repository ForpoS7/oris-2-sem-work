module org.example.mafia {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.itis.mafia to javafx.fxml;
    exports ru.itis.mafia;
}