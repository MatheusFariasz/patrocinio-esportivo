package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;
import org.openqa.selenium.WebDriver;

public class PaginaNaoEncontradaPageObject extends BasePageObject {
    private static final String TITULO = "Página não encontrada";

    public PaginaNaoEncontradaPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }
}
