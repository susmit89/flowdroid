package soot.jimple.infoflow.methodSummary.data.factory;

import soot.ArrayType;
import soot.SootField;
import soot.Type;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;

/**
 * Factory class for source and sink data objects
 * 
 * @author Steven Arzt
 */
public class SourceSinkFactory {

	private final int summaryAPLength;

	public SourceSinkFactory(int summaryAPLength) {
		this.summaryAPLength = summaryAPLength;
	}

	/**
	 * Cuts the given array of fields to the maximum access path length
	 * 
	 * @param fields
	 *            The array of fields to cut
	 * @return The cut array of fields
	 */
	private SootField[] cutAPLength(SootField[] fields) {
		if (fields == null || fields.length == 0)
			return null;
		if (fields.length <= summaryAPLength)
			return fields;

		SootField f[] = new SootField[summaryAPLength];
		System.arraycopy(fields, 0, f, 0, summaryAPLength);
		return f;
	}

	/**
	 * Cuts the given array of types to the maximum access path length
	 * 
	 * @param fields
	 *            The array of types to cut
	 * @return The cut array of types
	 */
	private Type[] cutAPLength(Type[] fields) {
		if (fields == null || fields.length == 0)
			return null;
		if (fields.length <= summaryAPLength)
			return fields;

		Type f[] = new Type[summaryAPLength];
		System.arraycopy(fields, 0, f, 0, summaryAPLength);
		return f;
	}

	public FlowSource createParameterSource(int parameterIdx, String baseType) {
		return new FlowSource(SourceSinkType.Parameter, parameterIdx, baseType);
	}

	public FlowSource createThisSource(String baseType) {
		return new FlowSource(SourceSinkType.Field, baseType);
	}

	/**
	 * Creates a new source model based on the given information
	 * 
	 * @param type
	 *            The type of data flow source
	 * @param parameterIdx
	 *            The index of the method parameter through which the source value
	 *            was obtained
	 * @param accessPath
	 *            The access path describing the exact source object
	 * @param gap
	 *            The gap from which this flow originated. Null if this flow did not
	 *            originate from a gap.
	 * @return The newly created source object
	 */
	public FlowSource createSource(SourceSinkType type, int parameterIdx, AccessPath accessPath, GapDefinition gap) {
		return new FlowSource(type, parameterIdx, accessPath.getBaseType().toString(),
				sootFieldsToString(cutAPLength(accessPath.getFields())),
				sootTypesToString(cutAPLength(accessPath.getFieldTypes())), gap);
	}

	/**
	 * Creates a sink that models a value assigned to a field reachable through a
	 * parameter value
	 * 
	 * @param paraIdx
	 *            The index of the parameter
	 * @param accessPath
	 *            The access path modeling the field inside the parameter value
	 * @return The sink object
	 */
	public FlowSink createParameterSink(int paraIdx, AccessPath accessPath) {
		return createParameterSink(paraIdx, accessPath, null);
	}

	/**
	 * Creates a sink that models a value assigned to a field reachable through a
	 * parameter value
	 * 
	 * @param paraIdx
	 *            The index of the parameter
	 * @param accessPath
	 *            The access path modeling the field inside the parameter value
	 * @param gap
	 *            The gap in whose parameter the flow ends
	 * @return The sink object
	 */
	public FlowSink createParameterSink(int paraIdx, AccessPath accessPath, GapDefinition gap) {
		if (accessPath.isLocal()) {
			if (gap == null && !accessPath.getTaintSubFields() && !(accessPath.getBaseType() instanceof ArrayType))
				throw new RuntimeException("Parameter locals cannot directly be sinks");
			else
				return new FlowSink(SourceSinkType.Parameter, paraIdx, accessPath.getBaseType().toString(),
						accessPath.getTaintSubFields(), gap);
		} else if (accessPath.getFieldCount() < summaryAPLength)
			return new FlowSink(SourceSinkType.Parameter, paraIdx, accessPath.getBaseType().toString(),
					sootFieldsToString(accessPath.getFields()), sootTypesToString(accessPath.getFieldTypes()),
					accessPath.getTaintSubFields(), gap);
		else
			return new FlowSink(SourceSinkType.Parameter, paraIdx, accessPath.getBaseType().toString(),
					sootFieldsToString(cutAPLength(accessPath.getFields())),
					sootTypesToString(cutAPLength(accessPath.getFieldTypes())), true, gap);
	}

	/**
	 * Creates a sink that models the base object of a call to a gap method. This is
	 * used to identify the object on which the gap method is invoked, so no access
	 * path is needed.
	 * 
	 * @param gap
	 *            The gap in whose base object the flow ends
	 * @return The sink object
	 */
	public FlowSink createGapBaseObjectSink(GapDefinition gap, Type baseType) {
		return new FlowSink(SourceSinkType.GapBaseObject, -1, baseType.toString(), false, gap);
	}

	/**
	 * Creates a sink that models the value returned by the method or a field
	 * reachable through the return value.
	 * 
	 * @param accessPath
	 *            The access path modeling the returned value
	 * @return The sink object
	 */
	public FlowSink createReturnSink(AccessPath accessPath) {
		return createReturnSink(accessPath, null);
	}

	/**
	 * Creates a sink that models the value returned by the method or a field
	 * reachable through the return value.
	 * 
	 * @param accessPath
	 *            The access path modeling the returned value
	 * @param gap
	 *            The gap to whose return value the data flows
	 * @return The sink object
	 */
	public FlowSink createReturnSink(AccessPath accessPath, GapDefinition gap) {
		if (accessPath.isLocal())
			return new FlowSink(SourceSinkType.Return, -1, accessPath.getBaseType().toString(),
					accessPath.getTaintSubFields(), gap);
		else if (accessPath.getFieldCount() < summaryAPLength)
			return new FlowSink(SourceSinkType.Return, -1, accessPath.getBaseType().toString(),
					sootFieldsToString(accessPath.getFields()), sootTypesToString(accessPath.getFieldTypes()),
					accessPath.getTaintSubFields(), gap);
		else
			return new FlowSink(SourceSinkType.Return, -1, accessPath.getBaseType().toString(),
					sootFieldsToString(cutAPLength(accessPath.getFields())),
					sootTypesToString(cutAPLength(accessPath.getFieldTypes())), true, gap);
	}

	/**
	 * Creates a sink that models a value assigned to a field
	 * 
	 * @param accessPath
	 *            The access path modeling the field
	 * @return The sink object
	 */
	public FlowSink createFieldSink(AccessPath accessPath) {
		return createFieldSink(accessPath, null);
	}

	/**
	 * Creates a sink that models a value assigned to a field
	 * 
	 * @param accessPath
	 *            The access path modeling the field
	 * @param gap
	 *            The gap in which this field taint ends
	 * @return The sink object
	 */
	public FlowSink createFieldSink(AccessPath accessPath, GapDefinition gap) {
		if (accessPath.isLocal())
			return new FlowSink(SourceSinkType.Field, -1, accessPath.getBaseType().toString(),
					accessPath.getTaintSubFields(), gap);
		else if (accessPath.getFieldCount() < summaryAPLength)
			return new FlowSink(SourceSinkType.Field, -1, accessPath.getBaseType().toString(),
					sootFieldsToString(accessPath.getFields()), sootTypesToString(accessPath.getFieldTypes()),
					accessPath.getTaintSubFields(), gap);
		else
			return new FlowSink(SourceSinkType.Field, -1, accessPath.getBaseType().toString(),
					sootFieldsToString(cutAPLength(accessPath.getFields())),
					sootTypesToString(cutAPLength(accessPath.getFieldTypes())), true, gap);
	}

	/**
	 * Converts an array of Soot fields into a string representation
	 * 
	 * @param fields
	 *            The array of Soot fields to convert
	 * @return The string representation of the given Soot fields
	 */
	private String[] sootFieldsToString(SootField[] fields) {
		if (fields == null || fields.length == 0)
			return null;

		String[] res = new String[fields.length];
		for (int i = 0; i < fields.length; i++)
			res[i] = fields[i].toString();
		return res;
	}

	/**
	 * Converts an array of Soot types into a string representation
	 * 
	 * @param fields
	 *            The array of Soot types to convert
	 * @return The string representation of the given Soot types
	 */
	private String[] sootTypesToString(Type[] types) {
		if (types == null || types.length == 0)
			return null;

		String[] res = new String[types.length];
		for (int i = 0; i < types.length; i++)
			res[i] = types[i].toString();
		return res;
	}

	/**
	 * Creates a sink that models a value assigned to a field reachable through a
	 * parameter value. This variant models a gap inside the method to be summarized
	 * 
	 * @param paraIdx
	 *            The index of the parameter
	 * @param accessPath
	 *            The access path modeling the field inside the parameter value
	 * @return The sink object
	 */
	public FlowSink createParameterSinkAtGap(int paraIdx, AccessPath accessPath, String gapSignature) {
		if (accessPath.isLocal()) {
			if (!(accessPath.getBaseType() instanceof ArrayType))
				throw new RuntimeException("Parameter locals cannot directly be sinks");
			else
				return new FlowSink(SourceSinkType.Parameter, paraIdx, accessPath.getBaseType().toString(),
						accessPath.getTaintSubFields());
		} else if (accessPath.getFieldCount() < summaryAPLength)
			return new FlowSink(SourceSinkType.Parameter, paraIdx, accessPath.getBaseType().toString(),
					sootFieldsToString(accessPath.getFields()), sootTypesToString(accessPath.getFieldTypes()),
					accessPath.getTaintSubFields());
		else
			return new FlowSink(SourceSinkType.Parameter, paraIdx, accessPath.getBaseType().toString(),
					sootFieldsToString(cutAPLength(accessPath.getFields())),
					sootTypesToString(cutAPLength(accessPath.getFieldTypes())), true);
	}

	/**
	 * Creates a custom. The semantics must be defined by the code that uses the
	 * sink
	 * 
	 * @param paraIdx
	 *            The index of the parameter
	 * @param accessPath
	 *            The access path modeling the field inside the parameter value
	 * @param gap
	 *            The gap in whose parameter the flow ends
	 * @param userData
	 *            Additional user data to be associated with the sink
	 * @return The sink object
	 */
	public FlowSink createCustomSink(int paraIdx, AccessPath accessPath, GapDefinition gap, Object userData) {
		if (accessPath.isLocal()) {
			if (gap == null && !accessPath.getTaintSubFields() && !(accessPath.getBaseType() instanceof ArrayType))
				throw new RuntimeException("Parameter locals cannot directly be sinks");
			else
				return new FlowSink(SourceSinkType.Custom, paraIdx, accessPath.getBaseType().toString(),
						accessPath.getTaintSubFields(), gap, userData);
		} else if (accessPath.getFieldCount() < summaryAPLength)
			return new FlowSink(SourceSinkType.Custom, paraIdx, accessPath.getBaseType().toString(),
					sootFieldsToString(accessPath.getFields()), sootTypesToString(accessPath.getFieldTypes()),
					accessPath.getTaintSubFields(), gap, userData);
		else
			return new FlowSink(SourceSinkType.Custom, paraIdx, accessPath.getBaseType().toString(),
					sootFieldsToString(cutAPLength(accessPath.getFields())),
					sootTypesToString(cutAPLength(accessPath.getFieldTypes())), true, gap, userData);
	}

}
