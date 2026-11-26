package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ErroContratoPageObject extends BasePageObject {
    private static final String TITULO = "Não foi possível abrir o contrato.";

    public ErroContratoPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }

    public String mensagemErro()
    {
        return aguardaElemento(By.cssSelector("[role='alert']")).getText();
    }
}
