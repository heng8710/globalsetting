package conf.attribute;

/**
 * 表示一般的属性引用，格式是这样：【.xxx】
 */
public final class Attribute implements IAttribute{

	public final String name;

	Attribute(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Attribute [name=" + name + "]";
	}

}
