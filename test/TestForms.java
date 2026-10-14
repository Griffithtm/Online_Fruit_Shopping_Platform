import model.PromotionForm;
import java.util.Map;
public final class TestForms {
 public static PromotionForm promotion(Map<String,String> values) {
 PromotionForm form=new PromotionForm();
 form.setId(values.get("id"));
 form.setAction(values.get("action"));
 form.setState(values.get("state"));
 form.setName(values.get("name"));
 form.setDiscountType(values.get("discountType"));
 form.setValue(values.get("value"));
 form.setBudget(values.get("budget"));
 form.setMinimum(values.get("minimum"));
 form.setLimit(values.get("limit"));
 form.setPerCustomer(values.get("perCustomer"));
 form.setStart(values.get("start"));
 form.setEnd(values.get("end"));
 form.setCode(values.get("code"));
 form.setListings(values.get("listings"));
 return form;
 }
}
