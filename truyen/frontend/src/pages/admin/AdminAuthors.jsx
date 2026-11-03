import { admin, catalog } from '../../api/index.js'
import SimpleCrud from './SimpleCrud.jsx'

export default function AdminAuthors() {
  return (
    <SimpleCrud
      title="Tác giả"
      noun="tác giả"
      queryKey={['authors']}
      list={catalog.authors}
      create={admin.createAuthor}
      update={admin.updateAuthor}
      remove={admin.deleteAuthor}
      textField="biography"
      textLabel="Tiểu sử"
    />
  )
}
