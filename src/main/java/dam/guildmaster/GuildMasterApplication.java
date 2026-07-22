package dam.guildmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GuildMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuildMasterApplication.class, args);
        System.out.println("||    GUILD MASTER API STARTED      ||");
    }
}
