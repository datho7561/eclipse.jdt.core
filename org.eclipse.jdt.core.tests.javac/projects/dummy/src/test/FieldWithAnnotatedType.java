package test;

class FieldWithAnnotatedType {
	java.util.List<@Marker Object> o;
}
@Target({ TYPE_USE })
public @interface Marker {
	// marker annotation with no members
}