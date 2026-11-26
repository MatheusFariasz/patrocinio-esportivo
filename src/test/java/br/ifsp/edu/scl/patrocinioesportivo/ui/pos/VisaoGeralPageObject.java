package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class VisaoGeralPageObject extends BasePageObject {
    private static final String TITULO = "Parcerias em movimento.";

    public VisaoGeralPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }

    public void consultar(String numero)
    {
        driver.findElement(By.name("id")).sendKeys(numero);
        driver.findElement(By.cssSelector("form button")).click();
    }

    public NovaPropostaPageObject navegarNovaProposta()
    {
        driver.findElement(By.partialLinkText("Nova proposta")).click();
        return new NovaPropostaPageObject(driver);
    }
}
