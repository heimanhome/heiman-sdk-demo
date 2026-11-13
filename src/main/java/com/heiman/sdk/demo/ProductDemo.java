package com.heiman.sdk.demo;

import com.heiman.sdk.restful.client.HeimanRestfulClient;
import com.heiman.sdk.restful.client.service.ProductService;
import com.heiman.sdk.restful.model.ClientConfig;
import com.heiman.sdk.restful.model.product.ProductInfo;

import java.util.List;

/**
 * 产品管理演示
 * 演示通过 SDK 查询产品列表。
 * Product management demo, demonstrates querying the product list through the SDK.
 * <p>
 * 运行凭证来自 classpath 下的 sdk-config.properties（缺失时回退到 SDK 内置示例值）。
 * Credentials come from sdk-config.properties on the classpath (falls back to SDK built-in placeholders).
 */
public class ProductDemo {

    public static void main(String[] args) throws Exception {
        new ProductDemo().run();
    }

    /**
     * 运行产品管理演示
     */
    public void run() throws Exception {
        System.out.println("=== Product Demo Startup ===");

        ClientConfig clientConfig = SdkConfig.get().toClientConfig();
        HeimanRestfulClient client = new HeimanRestfulClient(clientConfig);

        try {
            ProductService productService = client.getProductService();
            List<ProductInfo> products = productService.getProductList();

            System.out.println("Product list query successful, total: " + products.size());
            for (ProductInfo product : products) {
                System.out.println("  - id=" + product.getId()
                        + ", name=" + product.getName()
                        + ", classifiedName=" + product.getClassifiedName());
                System.out.println("product metadata: {} " + product.getMetadata());
            }
        } finally {
            client.close();
        }

        System.out.println("=== Product Demo Completed ===");
    }
}
