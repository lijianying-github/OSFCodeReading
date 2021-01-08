package di.model;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/8
 */
public class ModelCWithA {

    private ModelA mA;

    public String name="ModelCWithA";

    public ModelCWithA(ModelA mA) {
        this.mA = mA;
    }

    @Override
    public String toString() {
        return "ModelCWithA{" +
                "mA=" + mA +
                ", name='" + name + '\'' +
                '}';
    }
}
