package za.co.csnx.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import za.co.csnx.engine.common.BaseEntity;

/**
 * One smart-navigation request: the prompt, the chosen workflow, the outcome +
 * result count, whether a repair round ran, and whether the user acted. Feeds
 * accuracy/repair-rate metrics and synonym/example mining (the learning loop).
 */
@Entity
@Table(name = "assistant_query_log", schema = "demoschema")
public class AssistantQueryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "prompt", nullable = false, length = 1000)
    private String prompt;

    @Column(name = "workflow", length = 64)
    private String workflow;

    @Column(name = "outcome", nullable = false, length = 16)
    private String outcome;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "repaired", nullable = false)
    private boolean repaired;

    @Column(name = "acted")
    private Boolean acted;

    @Column(name = "company_code", length = 8)
    private String companyCode;

    @Column(name = "username", length = 64)
    private String username;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getWorkflow() { return workflow; }
    public void setWorkflow(String workflow) { this.workflow = workflow; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }

    public boolean isRepaired() { return repaired; }
    public void setRepaired(boolean repaired) { this.repaired = repaired; }

    public Boolean getActed() { return acted; }
    public void setActed(Boolean acted) { this.acted = acted; }

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
