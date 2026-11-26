package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPageObject extends BasePageObject {
    private static final String TITULO = "Bom ter você de volta.";

    public LoginPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }

    public void entrar(String email, String senha)
    {
        driver.findElement(By.name("email")).sendKeys(email);
        driver.findElement(By.name("password")).sendKeys(senha);
        driver.findElement(By.cssSelector("form button")).click();
    }

    public CadastroPageObject navegarCadastro()
    {
        driver.findElement(By.linkText("Cadastre-se")).click();
        return new CadastroPageObject(driver);
    }

    public String mensagemErro()
    {
        return aguardaElemento(By.cssSelector("[role='alert']")).getText();
    }

    public boolean arteVisivel()
    {
        return driver.findElement(By.tagName("aside")).isDisplayed();
    }
}
