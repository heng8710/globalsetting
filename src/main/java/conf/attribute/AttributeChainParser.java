package conf.attribute;

import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import conf.attribute.antlr.JsAttributeLexer;

/**
 * 用antlr进行解析属性引用链
 */
public final class AttributeChainParser {
	
	/**
	 * （只做token解析）
	 * @param s： 必须是【.】开头，后续都是这样的：.xx、[num]。
	 * @return
	 */
	public static List<IAttribute> parse(final String s){
		try {
			final List<IAttribute> attributeChain = Lists.newArrayList();
			
			final JsAttributeLexer lexer = new JsAttributeLexer(new ANTLRInputStream( s));
			final TokenSource ts = new CommonTokenStream(lexer).getTokenSource();
			for(Token t = ts.nextToken(); !Objects.equal(t.getType(), Token.EOF);t = ts.nextToken()){
				switch(t.getType()){
					case JsAttributeLexer.ATTR: {
						//.xxx
						final String text = t.getText();
						attributeChain.add(new Attribute(text.substring(1)));
						break;
					}
					
					case JsAttributeLexer.ARRATTR: {
						//[2223]
						final String text = t.getText();
						final String index = text.substring(1, text.length() - 1);
						attributeChain.add(new ArrayAttribute(Integer.valueOf(index)));
						break;
					}
					default: throw new IllegalStateException(String.format("属性链=[%s]不对，无法正常解析", s));
				}
				
			}
			return attributeChain;
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("属性链=[%s]不对，无法正常解析", s), e);
		}
	}

}
