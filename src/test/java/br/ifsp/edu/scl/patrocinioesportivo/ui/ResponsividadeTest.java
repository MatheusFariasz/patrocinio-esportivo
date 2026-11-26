package br.ifsp.edu.scl.patrocinioesportivo.ui;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.LoginPageObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
public class ResponsividadeTest extends BaseSeleniumTest
{
    @Test
    @DisplayName("Login em tela pequena esconde a arte")
    void loginEmTelaPequena()
    {
        driver.manage().window().setSize(new Dimension(600, 900));
        
        driver.get(URL_FRONT + "#/entrar");

        var login = new LoginPageObject(driver);

        assertThat(login.arteVisivel()).isFalse();
    }

    @Test
    @DisplayName("Login em tela grande mostra a arte")
    void loginEmTelaGrande()
    {
        driver.manage().window().setSize(new Dimension(1400, 900));

        driver.get(URL_FRONT + "#/entrar");

        var login = new LoginPageObject(driver);

        assertThat(login.arteVisivel()).isTrue();
    }
}
