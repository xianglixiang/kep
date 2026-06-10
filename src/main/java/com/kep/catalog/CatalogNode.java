package com.kep.catalog;

import com.kep.shared.jpa.TenantAwareEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "catalog_node")
public class CatalogNode extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path = "/";

    @Column(nullable = false)
    private int sort = 0;

    @Column(name = "node_type", nullable = false)
    private String nodeType = "FOLDER";

    // 列名显式指定为 private（privateNode 是 Java 字段名，避开关键字）
    @Column(name = "private", nullable = false)
    private boolean privateNode = false;

    protected CatalogNode() {}

    public static CatalogNode folder(Long parentId, String name, String path) {
        CatalogNode node = new CatalogNode();
        node.parentId = parentId;
        node.name = name;
        node.path = path;
        node.nodeType = "FOLDER";
        return node;
    }

    /** 仅供内存适配器赋 id；JPA 模式下由数据库 IDENTITY 生成。 */
    void assignId(Long id) {
        this.id = id;
    }

    /** 仅供 InMemoryCatalogNodeStore 与 JPA 适配器在 save 时设置 path。 */
    void assignPath(String path) { this.path = path; }

    public Long getId() { return id; }
    public Long getParentId() { return parentId; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public int getSort() { return sort; }
    public String getNodeType() { return nodeType; }
    public boolean isPrivateNode() { return privateNode; }
}
