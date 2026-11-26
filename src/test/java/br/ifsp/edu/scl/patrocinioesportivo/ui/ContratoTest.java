package br.ifsp.edu.scl.patrocinioesportivo.ui;

import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.ContratoPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.LoginPageObject;
import br.ifsp.edu.scl.patrocinioesportivo.ui.pos.VisaoGeralPageObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("UiTest")
public class ContratoTest extends BaseSeleniumTest {
    @Override
    protected void setInitialPage()
    {
        driver.get(URL_FRONT + "#/entrar");
    }

    private ContratoPageObject criarProposta(String email)
    {
        new LoginPageObject(driver).entrar(email, SENHA);
        var visaoGeral = new VisaoGeralPageObject(driver);

        visaoGeral.navegarNovaProposta().criar("10", "20", "6000", "100", "01012027", "01072027");
        return new ContratoPageObject(driver);
    }

    @Test
    @DisplayName("User comum nao deve aprovar proposta")
    void aprovarComoUsuarioComum()
    {
        var contrato = criarProposta(USUARIO);
        contrato.aprovar();

        assertThat(contrato.mensagemModal()).isEqualTo("Apenas o diretor financeiro pode aprovar propostas.");
    }

    @Test
    @DisplayName("Diretor financeiro aprova a proposta")
    void aprovarComoDiretorFinanceiro()
    {
        var contrato = criarProposta(DIRETOR);
        contrato.aprovar();

        assertThat(contrato.mensagemSucesso()).isEqualTo("Proposta aprovada. O contrato está ativo.");
        assertThat(contrato.status()).isEqualTo("Ativo");
    }
}
