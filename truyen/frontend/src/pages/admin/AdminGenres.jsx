import { admin, catalog } from '../../api/index.js'
import SimpleCrud from './SimpleCrud.jsx'

export default function AdminGenres() {
  return (
    <SimpleCrud
      title="Thể loại"
      noun="thể loại"
      queryKey={['genres']}
      list={catalog.genres}
      create={admin.createGenre}
      update={admin.updateGenre}
      remove={admin.deleteGenre}
      textField="description"
      textLabel="Mô tả"
      textLimit={500}
      renderMeta={(genre) => <code className="muted">/{genre.slug}</code>}
    />
  )
}
