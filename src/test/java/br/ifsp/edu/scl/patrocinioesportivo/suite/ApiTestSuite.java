package br.ifsp.edu.scl.patrocinioesportivo.suite;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Todos os testes de API")
@SelectPackages("br.ifsp.edu.scl.patrocinioesportivo")
@IncludeTags("ApiTest")
public class ApiTestSuite {
}
