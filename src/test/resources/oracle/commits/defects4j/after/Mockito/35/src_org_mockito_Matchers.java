/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matcher;
import org.mockito.internal.matchers.*;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.mockito.internal.progress.HandyReturnValues;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;

/**
 * Allow flexible verification or stubbing. See also {@link AdditionalMatchers}.
 * <p>
 * {@link Mockito} extends Matchers so to get access to all matchers just import Mockito class statically.
 * <pre>
 *  //stubbing using anyInt() argument matcher
 *  when(mockedList.get(anyInt())).thenReturn("element");
 *  
 *  //following prints "element"
 *  System.out.println(mockedList.get(999));
 *  
 *  //you can also verify using argument matcher
 *  verify(mockedList).get(anyInt());
 * </pre>
 * Scroll down to see all methods - full list of matchers.
 * <p>
 * <b>Warning:</b>
 * <p>
 * If you are using argument matchers, <b>all arguments</b> have to be provided by matchers.
 * <p>
 * E.g: (example shows verification but the same applies to stubbing):
 * <pre>
 *   verify(mock).someMethod(anyInt(), anyString(), <b>eq("third argument")</b>);
 *   //above is correct - eq() is also an argument matcher
 *   
 *   verify(mock).someMethod(anyInt(), anyString(), <b>"third argument"</b>);
 *   //above is incorrect - exception will be thrown because third argument is given without argument matcher.
 * </pre>
 * 
 * <h1>Custom Argument Matchers</h1>
 * 
 * Use {@link Matchers#argThat} method and pass an instance of hamcrest {@link Matcher}.
 * <p>
 * Before you start implementing your own custom argument matcher, make sure you check out {@link ArgumentCaptor} api.
 * <p>
 * So, how to implement your own argument matcher?
 * First, you might want to subclass {@link ArgumentMatcher} which is an hamcrest matcher with predefined describeTo() method.
 * Default description generated by describeTo() uses <b>decamelized class name</b> - to promote meaningful class names.
 * <p>
 * Example:
 * 
 * <pre>
 *   class IsListOfTwoElements extends ArgumentMatcher&lt;List&gt; {
 *      public boolean matches(Object list) {
 *          return ((List) list).size() == 2;
 *      }
 *   }
 *   
 *   List mock = mock(List.class);
 *   
 *   when(mock.addAll(argThat(new IsListOfTwoElements()))).thenReturn(true);
 *   
 *   mock.addAll(Arrays.asList("one", "two"));
 *   
 *   verify(mock).addAll(argThat(new IsListOfTwoElements()));
 * </pre>
 * 
 * To keep it readable you may want to extract method, e.g:
 * <pre>
 *   verify(mock).addAll(<b>argThat(new IsListOfTwoElements())</b>);
 *   //becomes
 *   verify(mock).addAll(<b>listOfTwoElements()</b>);
 * </pre>
 *
 * <b>Warning:</b> Be reasonable with using complicated argument matching, especially custom argument matchers, as it can make the test less readable. 
 * Sometimes it's better to implement equals() for arguments that are passed to mocks 
 * (Mockito naturally uses equals() for argument matching). 
 * This can make the test cleaner. 
 * <p>
 * Also, <b>sometimes {@link ArgumentCaptor} may be a better fit</b> than custom matcher. 
 * For example, if custom argument matcher is not likely to be reused
 * or you just need it to assert on argument values to complete verification of behavior.
 */
@SuppressWarnings("unchecked")
public class Matchers {
    
    private static MockingProgress mockingProgress = new ThreadSafeMockingProgress();

    /**
     * any boolean, Boolean or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>false</code>.
     */
    public static boolean anyBoolean() {
        return reportMatcher(Any.ANY).returnFalse();
    }

    /**
     * any byte, Byte or null
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>0</code>.
     */
    public static byte anyByte() {
        return reportMatcher(Any.ANY).returnZero();
    }

    /**
     * any char, Character or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>0</code>.
     */
    public static char anyChar() {
        return reportMatcher(Any.ANY).returnChar();
    }

    /**
     * any int, Integer or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>0</code>.
     */
    public static int anyInt() {
        return reportMatcher(Any.ANY).returnZero();
    }

    /**
     * any long, Long or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>0</code>.
     */
    public static long anyLong() {
        return reportMatcher(Any.ANY).returnZero();
    }

    /**
     * any float, Float or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>0</code>.
     */
    public static float anyFloat() {
        return reportMatcher(Any.ANY).returnZero();
    }

    /**
     * any double, Double or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>0</code>.
     */
    public static double anyDouble() {
        return reportMatcher(Any.ANY).returnZero();
    }

    /**
     * any short, Short or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>0</code>.
     */
    public static short anyShort() {
        return reportMatcher(Any.ANY).returnZero();
    }

    /**
     * any Object or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>null</code>.
     */
    public static <T> T anyObject() {
        return (T) reportMatcher(Any.ANY).returnNull();
    }
    //TODO: after 1.8 check out Jay Fields' idea on any() matcher

    /**
     * Any vararg, meaning any number and values of arguments.
     * <p>
     * Example:
     * <pre>
     *   //verification:
     *   mock.foo(1, 2);
     *   mock.foo(1, 2, 3, 4);
     *
     *   verify(mock, times(2)).foo(anyVararg());
     *
     *   //stubbing:
     *   when(mock.foo(anyVararg()).thenReturn(100);
     *
     *   //prints 100
     *   System.out.println(mock.foo(1, 2));
     *   //also prints 100
     *   System.out.println(mock.foo(1, 2, 3, 4));
     * </pre>
     * See examples in javadoc for {@link Matchers} class
     *
     * @return <code>null</code>.
     */
    public static <T> T anyVararg() {
        return (T) reportMatcher(AnyVararg.ANY_VARARG).returnNull();
    }
    
    /**
     * any kind object, not necessary of the given class.
     * The class argument is provided only to avoid casting.
     * <p>
     * Sometimes looks better than anyObject() - especially when explicit casting is required
     * <p>
     * Alias to {@link Matchers#anyObject()}
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>null</code>.
     */
    public static <T> T any(Class<T> clazz) {
        return (T) anyObject();
    }
    
    /**
     * any object or null 
     * <p>
     * Shorter alias to {@link Matchers#anyObject()}
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>null</code>.
     */
    public static <T> T any() {
        return (T) anyObject();
    }

    /**
     * any String or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return empty String ("")
     */
    public static String anyString() {
        return reportMatcher(Any.ANY).returnString();
    }
    
    /**
     * any List or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return empty List.
     */
    public static List anyList() {
        return reportMatcher(Any.ANY).returnList();
    }    
    
    /**
     * generic friendly alias to {@link Matchers#anyList()}.
     * It's an alternative to &#064;SuppressWarnings("unchecked") to keep code clean of compiler warnings.
     * <p>
     * any List or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return empty List.
     */
    public static <T> List<T> anyListOf(Class<T> clazz) {
        return (List) reportMatcher(Any.ANY).returnList();
    }    
    
    /**
     * any Set or null
     * <p>
     * See examples in javadoc for {@link Matchers} class
     *
     * @return empty Set
     */
    public static Set anySet() {
        return reportMatcher(Any.ANY).returnSet();
    }
    
    /**
     * generic friendly alias to {@link Matchers#anySet()}.
     * It's an alternative to &#064;SuppressWarnings("unchecked") to keep code clean of compiler warnings.
     * <p>
     * any Set or null
     * <p>
     * See examples in javadoc for {@link Matchers} class
     *
     * @return empty Set
     */
    public static <T> Set<T> anySetOf(Class<T> clazz) {
        return (Set) reportMatcher(Any.ANY).returnSet();
    }

    /**
     * any Map or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return empty Map.
     */
    public static Map anyMap() {
        return reportMatcher(Any.ANY).returnMap();
    }    
    
    /**
     * any Collection or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return empty Collection.
     */
    public static Collection anyCollection() {
        return reportMatcher(Any.ANY).returnList();
    }    
    
    /**
     * generic friendly alias to {@link Matchers#anyCollection()}. 
     * It's an alternative to &#064;SuppressWarnings("unchecked") to keep code clean of compiler warnings.     
     * <p>
     * any Collection or null.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return empty Collection.
     */
    public static <T> Collection<T> anyCollectionOf(Class<T> clazz) {
        return (Collection) reportMatcher(Any.ANY).returnList();
    }    

    /**
     * Object argument that implements the given class. 
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param <T>
     *            the accepted type.
     * @param clazz
     *            the class of the accepted type.
     * @return <code>null</code>.
     */
    public static <T> T isA(Class<T> clazz) {
        return reportMatcher(new InstanceOf(clazz)).<T>returnFor(clazz);
    }

    /**
     * boolean argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static boolean eq(boolean value) {
        return reportMatcher(new Equals(value)).returnFalse();
    }

    /**
     * byte argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static byte eq(byte value) {
        return reportMatcher(new Equals(value)).returnZero();
    }

    /**
     * char argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static char eq(char value) {
        return reportMatcher(new Equals(value)).returnChar();
    }

    /**
     * double argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static double eq(double value) {
        return reportMatcher(new Equals(value)).returnZero();
    }

    /**
     * float argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static float eq(float value) {
        return reportMatcher(new Equals(value)).returnZero();
    }
    
    /**
     * int argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static int eq(int value) {
        return reportMatcher(new Equals(value)).returnZero();
    }

    /**
     * long argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static long eq(long value) {
        return reportMatcher(new Equals(value)).returnZero();
    }

    /**
     * short argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>0</code>.
     */
    public static short eq(short value) {
        return reportMatcher(new Equals(value)).returnZero();
    }

    /**
     * Object argument that is equal to the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @return <code>null</code>.
     */
    public static <T> T eq(T value) {
        return (T) reportMatcher(new Equals(value)).<T>returnFor((Class) value.getClass());
    }  

    /**
     * Object argument that is reflection-equal to the given value with support for excluding
     * selected fields from a class.
     * <p>
     * This matcher can be used when equals() is not implemented on compared objects.
     * Matcher uses java reflection API to compare fields of wanted and actual object.
     * <p>
     * Works similarly to EqualsBuilder.reflectionEquals(this, other, exlucdeFields) from
     * apache commons library.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param value
     *            the given value.
     * @param excludeFields
     *            fields to exclude, if field does not exist it is ignored.
     * @return <code>null</code>.
     */
    public static <T> T refEq(T value, String... excludeFields) {
        return reportMatcher(new ReflectionEquals(value, excludeFields)).<T>returnNull();
    }
    
    /**
     * Object argument that is the same as the given value.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param <T>
     *            the type of the object, it is passed through to prevent casts.
     * @param value
     *            the given value.
     * @return <code>null</code>.
     */
    public static <T> T same(T value) {
        return (T) reportMatcher(new Same(value)).<T>returnFor((Class) value.getClass());
    }

    /**
     * null argument.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>null</code>.
     */
    public static Object isNull() {
        return reportMatcher(Null.NULL).returnNull();
    }

    /**
     * not null argument.
     * <p>
     * alias to {@link Matchers#isNotNull()}
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>null</code>.
     */
    public static Object notNull() {
        return reportMatcher(NotNull.NOT_NULL).returnNull();
    }
    
    /**
     * not null argument.
     * <p>
     * alias to {@link Matchers#notNull()}
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @return <code>null</code>.
     */
    public static Object isNotNull() {
        return notNull();
    }

    /**
     * String argument that contains the given substring.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param substring
     *            the substring.
     * @return empty String ("").
     */
    public static String contains(String substring) {
        return reportMatcher(new Contains(substring)).returnString();
    }

    /**
     * String argument that matches the given regular expression.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param regex
     *            the regular expression.
     * @return empty String ("").
     */
    public static String matches(String regex) {
        return reportMatcher(new Matches(regex)).returnString();
    }

    /**
     * String argument that ends with the given suffix.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param suffix
     *            the suffix.
     * @return empty String ("").
     */
    public static String endsWith(String suffix) {
        return reportMatcher(new EndsWith(suffix)).returnString();
    }

    /**
     * String argument that starts with the given prefix.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param prefix
     *            the prefix.
     * @return empty String ("").
     */
    public static String startsWith(String prefix) {
        return reportMatcher(new StartsWith(prefix)).returnString();
    }

    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link ArgumentMatcher} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>null</code>.
     */
    public static <T> T argThat(Matcher<T> matcher) {
        return reportMatcher(matcher).<T>returnNull();
    }
    
    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>0</code>.
     */
    public static char charThat(Matcher<Character> matcher) {
        return reportMatcher(matcher).returnChar();
    }
    
    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>false</code>.
     */
    public static boolean booleanThat(Matcher<Boolean> matcher) {
        return reportMatcher(matcher).returnFalse();
    }
    
    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>0</code>.
     */
    public static byte byteThat(Matcher<Byte> matcher) {
        return reportMatcher(matcher).returnZero();
    }
    
    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>0</code>.
     */
    public static short shortThat(Matcher<Short> matcher) {
        return reportMatcher(matcher).returnZero();
    }
    
    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>0</code>.
     */
    public static int intThat(Matcher<Integer> matcher) {
        return reportMatcher(matcher).returnZero();
    }

    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>0</code>.
     */
    public static long longThat(Matcher<Long> matcher) {
        return reportMatcher(matcher).returnZero();
    }
    
    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>0</code>.
     */
    public static float floatThat(Matcher<Float> matcher) {
        return reportMatcher(matcher).returnZero();
    }
    
    /**
     * Allows creating custom argument matchers.
     * <p>
     * See examples in javadoc for {@link Matchers} class
     * 
     * @param matcher decides whether argument matches
     * @return <code>0</code>.
     */
    public static double doubleThat(Matcher<Double> matcher) {
        return reportMatcher(matcher).returnZero();
    }

    private static HandyReturnValues reportMatcher(Matcher<?> matcher) {
        return mockingProgress.getArgumentMatcherStorage().reportMatcher(matcher);
    }
}