package br.ifsp.edu.scl.patrocinioesportivo.ui.pos;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CadastroPageObject extends BasePageObject {
    private static final String TITULO = "Faça parte do jogo.";

    public CadastroPageObject(WebDriver driver)
    {
        super(driver);
        aguardaTitulo(TITULO);
    }

    public void cadastrar(String nome, String sobrenome, String email, String senha, String confirmacao)
    {
        driver.findElement(By.name("name")).sendKeys(nome);
        driver.findElement(By.name("lastname")).sendKeys(sobrenome);
        driver.findElement(By.name("email")).sendKeys(email);
        driver.findElement(By.name("password")).sendKeys(senha);
        driver.findElement(By.name("confirm")).sendKeys(confirmacao);
        driver.findElement(By.cssSelector("form button")).click();
    }

    public String mensagemErro()
    {
        return aguardaElemento(By.cssSelector("[role='alert']")).getText();
    }

    public LoginPageObject navegarLogin()
    {
        driver.findElement(By.linkText("Entre aqui")).click();
        return new LoginPageObject(driver);
    }
}
