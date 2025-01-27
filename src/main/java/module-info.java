module org.example.mafia {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.itis.mafia to javafx.fxml;
    exports ru.itis.mafia;
    exports ru.itis.mafia.inter;
    opens ru.itis.mafia.inter to javafx.fxml;
    exports ru.itis.mafia.impl;
    opens ru.itis.mafia.impl to javafx.fxml;
}