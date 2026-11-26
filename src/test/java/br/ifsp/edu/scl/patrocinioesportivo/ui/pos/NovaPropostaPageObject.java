package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class NovaPropostaPageObject extends BasePageObject 
{
    private static final String TITULO = "Vamos construir uma parceria.";

    public NovaPropostaPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }

    public void criar(String clube, String patrocinador, String valor, String meta, String inicio, String termino)
    {
        driver.findElement(By.name("clubeId")).sendKeys(clube);
        driver.findElement(By.name("patrocinadorId")).sendKeys(patrocinador);
        driver.findElement(By.name("valor")).sendKeys(valor);
        driver.findElement(By.name("meta")).sendKeys(meta);
        driver.findElement(By.name("inicio")).sendKeys(inicio);
        driver.findElement(By.name("termino")).sendKeys(termino);
        driver.findElement(By.cssSelector("form button")).click();
    }

    public String mensagemErro()
    {
        return aguardaElemento(By.cssSelector("[role='alert']")).getText();
    }
}
