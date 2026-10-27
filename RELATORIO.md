# Relatório Técnico — Refatoração do Núcleo de Cobrança (SaaS Billing Co.)

**Disciplina:** Implantação e Entrega de Software — INF/UFG, 2026/2  
**Professora:** Sofia Costa Paiva  
**Dupla:** _[Nome 1 — matrícula]_ e _[Nome 2 — matrícula]_

## 1. Como executar

O projeto usa Java 17 e não possui `pom.xml`. Os testes executáveis usam apenas o JDK:

```bash
javac --release 17 -d /tmp/ines-billing-testes src/main/java/com/saas/billing/*.java src/main/java/com/saas/billing/infrastructure/*.java src/test/java/com/saas/billing/*.java
java -ea -cp /tmp/ines-billing-testes com.saas.billing.BillingTests
```

## 2. Matriz de Code Smells e Violações Arquiteturais

As linhas abaixo são do arquivo original `SubscriptionBillingService-pratica.java`, no commit `7fcc313`.

| Trecho ou linhas afetadas | Code Smell ou princípio violado | Problema identificado | Juros do débito técnico | Como foi resolvido |
|---|---|---|---|---|
| L18–105: `processMonthlyBilling` | **Long Method** | O método reúne cálculo, condicionais, consulta, cobrança, gravação e notificação. | Uma mudança pequena exige revisar muitos fluxos; aumenta o risco de regressão e dificulta testes e revisão no CI. | O método principal passou a orquestrar a cobrança em 9 linhas físicas; o fluxo do cliente foi extraído e os cálculos foram para `PricingCalculator`. |
| L18–20 e L29–56: oito parâmetros e `double` | **Primitive Obsession** | Dados da cobrança chegam soltos e dinheiro é calculado com ponto flutuante. | É fácil trocar parâmetros do mesmo tipo e obter diferenças decimais difíceis de auditar. | `BillingCommand` reúne a entrada e `Money` usa `BigDecimal`; a sobrecarga antiga mantém compatibilidade com chamadas existentes. |
| L27–57 e L59–98: regras e operações externas | **Violação do Single Responsibility Principle (SRP)** | A classe calcula preços, consulta clientes, simula pagamentos, grava faturas e prepara notificações. | Mudar preço, banco ou e-mail exige editar o mesmo serviço e ampliar os testes de regressão. | `PricingCalculator` cuida do preço; repositórios, gateway e notificação recebem as demais responsabilidades. |
| L59–63, L75–87 e L89–96: JDBC, token e `SimpleEmail` | **Dependência direta de I/O e violação do Dependency Inversion Principle (DIP)** | O serviço cria conexão JDBC, contém a regra do token e configura e-mail diretamente. | Testes do fluxo dependem do código de infraestrutura; trocar implementações exige alterar o serviço. | O serviço usa `CustomerRepository`, `InvoiceRepository`, `PaymentGateway` e `NotificationService`; implementações ficam em `infrastructure`. O envio real de e-mail continua desativado, como no original. |
| L25: `LocalDate.now()` | **Dependência de data do sistema** | A data usada nas promoções e no tempo de assinatura depende do dia da execução. | Casos de fronteira mudam com o calendário e podem gerar testes instáveis no CI. | O serviço recebe `Clock` pelo construtor e usa `LocalDate.now(clock)`; o cálculo recebe a data como argumento. |
| L100–102: `catch (Exception e)` | **Tratamento inadequado de exceções** | Qualquer erro vira texto no retorno, sem a causa disponível ao chamador. | Falhas de banco podem passar despercebidas e são difíceis de diagnosticar ou monitorar. | Adaptadores JDBC capturam `SQLException` e lançam `BillingException` com a causa original; o serviço não captura exceções genéricas. |

## 3. Refatorações realizadas

| Commit | Alteração comprovada no Git |
|---|---|
| `98eecac` — Criando classes para valores e dados da cobranca | Criou `Money` imutável com `BigDecimal` e `BillingCommand` para agrupar dados da cobrança. |
| `4f03f3f` — Separando as interfaces dos servicos | Criou `Customer`, `CustomerRepository`, `InvoiceRepository`, `PaymentGateway` e `NotificationService`. |
| `015baae` — Separando os calculos de preco | Criou `PricingCalculator` e separou descontos, promoção, multa e juros em métodos sem banco ou rede. |
| `926861c` — Simplificando o metodo de cobranca | Substituiu o arquivo legado, injetou as dependências e `Clock`, reduziu `processMonthlyBilling(BillingCommand)` e manteve a assinatura antiga por sobrecarga. |
| `cd10133` — Organizando o acesso ao banco e tratamento de erros | Criou adaptadores JDBC, pagamento por prefixo de token e composição de e-mail; moveu credenciais para variáveis de ambiente, preservou causas SQL e adicionou testes. |

## 4. Métricas antes e depois

Comparação entre o arquivo legado no commit `7fcc313` e os arquivos de produção no commit `cd10133`. Linhas físicas incluem assinatura e chaves. Complexidade ciclomática contada manualmente como 1 + cada `if`/`else if`/`catch` explícito, sem contar operadores booleanos nem chamadas como `Optional.ifPresent`.

| Métrica | Antes | Depois | Como foi obtida |
|---|---:|---:|---|
| Linhas do método principal | 88 | 9 | L18–105 do legado; L28–36 de `processMonthlyBilling(BillingCommand)`. |
| Complexidade ciclomática do método principal | 13 | 1 | Legado: 11 condicionais `if` e 1 `catch`; novo método: nenhum desvio explícito. |
| Parâmetros do método principal | 8 | 1 | Assinaturas dos métodos; a sobrecarga de compatibilidade ainda recebe 8. |
| Responsabilidades da classe principal | 5 | 1 | Contagem qualitativa: preço, cliente, pagamento, fatura e notificação; depois, orquestração. |
| Dependências diretas de infraestrutura no serviço | 2 | 0 | Legado importa e usa JDBC e `SimpleEmail`; serviço atual depende de interfaces. |
| Chamadas diretas ao relógio do sistema | 1 | 0 | `LocalDate.now()` no legado; no serviço atual há `LocalDate.now(clock)`. |
| Blocos genéricos `catch (Exception e)` | 1 | 0 | Busca nos métodos de produção. |
| Classes e records de produção | 1 | 12 | Declarações `class`/`record` em `src/main/java`; há também 5 interfaces atuais. |
| Testes unitários executáveis | 0 | 4 | Quatro métodos `test...` chamados por `BillingTests.main`; o legado não tinha testes no repositório. |

O aumento no número de tipos resulta da separação de responsabilidades. Esta tabela não mede cobertura nem equivalência percentual com o legado.

## 5. Arquitetura final

`SubscriptionBillingService` orquestra a cobrança e depende de `PricingCalculator`, `Clock` e quatro interfaces: `CustomerRepository`, `InvoiceRepository`, `PaymentGateway` e `NotificationService`. `PricingCalculator` recebe os dados e a data e não acessa banco nem rede. Os adaptadores em `infrastructure` implementam JDBC, a validação simulada do token e a composição da notificação. `JdbcConnectionProvider` lê URL, usuário e senha de `BILLING_DB_URL`, `BILLING_DB_USER` e `BILLING_DB_PASSWORD`.
