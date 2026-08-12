import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog,
  DialogBody,
  DialogHeader,
  DialogTitle,
  Form,
  FormField,
  FormFooter,
  FormItem,
  FormLabel,
  Input,
  Select,
  SelectOption,
} from "@sun/components";
import { useState } from "react";
import { useHub } from "~/routes/hub/hub-context";

/**
 * Create/edit dialog for a hub app.
 */
const AppForm = () => {
  const { t } = useTranslation("hub");
  const { editing, creating, onSubmit, closeForm } = useHub();
  const [saving, setSaving] = useState(false);
  const open = creating || Boolean(editing);

  let submitLabel: string;
  if (saving) {
    submitLabel = t("form.saving");
  } else if (editing) {
    submitLabel = t("form.update");
  } else {
    submitLabel = t("form.create");
  }

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (saving) {
      return;
    }
    setSaving(true);
    const formData = new FormData(event.currentTarget);
    const key = String(formData.get("key") ?? "").trim() || editing?.key || "";
    onSubmit({
      key,
      name: String(formData.get("name") ?? "").trim() || key,
      dir: String(formData.get("dir") ?? "").trim(),
      devPort: Number(formData.get("devPort")) || 5173,
      prodPort: Number(formData.get("prodPort")) || 5173,
      url: String(formData.get("url") ?? "").trim(),
      description: String(formData.get("description") ?? "").trim(),
      enabled: formData.get("enabled") === "true",
      self: editing?.self,
    });
  };

  return (
    <Dialog open={open} onOpenChange={(o: boolean) => !o && closeForm()}>
      <DialogHeader>
        <DialogTitle>
          {editing ? t("form.update") : t("form.create")}
        </DialogTitle>
      </DialogHeader>
      <DialogBody>
        <Form onSubmit={handleSubmit}>
          <FormField name="key">
            <FormLabel>{t("form.key")}</FormLabel>
            <FormItem>
              <Input
                type="text"
                defaultValue={editing?.key}
                disabled={Boolean(editing)}
                required
              />
            </FormItem>
          </FormField>
          <FormField name="name">
            <FormLabel>{t("form.name")}</FormLabel>
            <FormItem>
              <Input type="text" defaultValue={editing?.name} required />
            </FormItem>
          </FormField>
          <FormField name="dir">
            <FormLabel>{t("form.dir")}</FormLabel>
            <FormItem>
              <Input type="text" defaultValue={editing?.dir} />
            </FormItem>
          </FormField>
          <FormField name="devPort">
            <FormLabel>{t("form.devPort")}</FormLabel>
            <FormItem>
              <Input type="number" defaultValue={editing?.devPort} />
            </FormItem>
          </FormField>
          <FormField name="prodPort">
            <FormLabel>{t("form.prodPort")}</FormLabel>
            <FormItem>
              <Input type="number" defaultValue={editing?.prodPort} />
            </FormItem>
          </FormField>
          <FormField name="url">
            <FormLabel>{t("form.url")}</FormLabel>
            <FormItem>
              <Input type="text" defaultValue={editing?.url} />
            </FormItem>
          </FormField>
          <FormField name="description">
            <FormLabel>{t("form.description")}</FormLabel>
            <FormItem>
              <Input type="text" defaultValue={editing?.description} />
            </FormItem>
          </FormField>
          <FormField name="enabled">
            <FormLabel>{t("form.enabled")}</FormLabel>
            <FormItem>
              <Select defaultValue={editing?.enabled ? "true" : "false"}>
                <SelectOption value="true">{t("form.yes")}</SelectOption>
                <SelectOption value="false">{t("form.no")}</SelectOption>
              </Select>
            </FormItem>
          </FormField>
          <FormFooter>
            <Button type="button" variant="secondary" onClick={closeForm}>
              {t("cancel")}
            </Button>
            <Button type="submit" disabled={saving}>
              {submitLabel}
            </Button>
          </FormFooter>
        </Form>
      </DialogBody>
    </Dialog>
  );
};

export default AppForm;
