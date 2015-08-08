package conf.attribute;

/**
 * 表示一般的数组属性引用，格式是这样：【[num]】
 */
public final class ArrayAttribute implements IAttribute{

	public final int index;

	ArrayAttribute(final int index) {
		this.index = index;
	}
	
	

	@Override
	public String toString() {
		return "ArrayAttribute [index=" + index + "]";
	}
	
}
