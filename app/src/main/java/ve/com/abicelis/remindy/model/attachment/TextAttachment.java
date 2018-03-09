package ve.com.abicelis.remindy.model.attachment;

import ve.com.abicelis.remindy.enums.AttachmentType;



public class TextAttachment extends Attachment {

    private String text;

    public TextAttachment(String text) {
        this.text = text;
    }
    public TextAttachment(int id, int reminderId, String text) {
        super(id, reminderId);
        this.text = text;
    }

    @Override
    public AttachmentType getType() {
        return AttachmentType.TEXT;
    }


    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}
