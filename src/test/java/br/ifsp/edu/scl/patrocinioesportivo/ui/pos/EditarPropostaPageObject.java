package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class EditarPropostaPageObject extends BasePageObject {
    private static final String TITULO = "Ajuste os detalhes.";

    public EditarPropostaPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }

    public String mensagemErro()
    {
        return aguardaElemento(By.cssSelector("[role='alert']")).getText();
    }
}
