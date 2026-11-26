package br.ifsp.edu.scl.patrocinioesportivo.ui;

import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.LoginPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.VisaoGeralPageObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
public class LoginTest extends BaseSeleniumTest {

    @Override
    protected void setInitialPage()
    {
        driver.get(URL_FRONT + "#/entrar");
    }

    @Test
    @DisplayName("Login com sucesso usando credenciais verdadeiras")
    void deveEntrarComCredenciaisValidas()
    {
        var login = new LoginPageObject(driver);
        login.entrar(USUARIO, SENHA);

        var visaoGeral = new VisaoGeralPageObject(driver);
        assertThat(visaoGeral.titulo()).isEqualTo("Parcerias em movimento.");
    }


    @Test
    @DisplayName("Login com senha errada mostra mensagem de erro")
    void naoDeveEntrarComSenhaErrada()
    {
        var login = new LoginPageObject(driver);
        login.entrar(USUARIO, "senhaErrada123");

        assertThat(login.mensagemErro()).isEqualTo("Credenciais inválidas.");
    }

    @Test
    @DisplayName("Link do login leva para a tela de cadastro")
    void deveNavegarParaCadastro()
    {
        var login = new LoginPageObject(driver);
        var cadastro = login.navegarCadastro();

        assertThat(cadastro.titulo()).isEqualTo("Faça parte do jogo.");
    }
}
