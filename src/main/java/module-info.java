module com.example.telecomsim {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.luben.zstd_jni;
    requires org.lz4.java;
    requires snappy.java;
    requires lombok;
    requires org.slf4j;


    opens com.example.telecomsim to javafx.fxml;
    opens com.example.telecomsim.ui.controller to javafx.fxml;
    exports com.example.telecomsim;
}